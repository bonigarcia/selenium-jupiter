/*
 * (C) Copyright 2017 Boni Garcia (http://bonigarcia.github.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.bonigarcia.seljup;

import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.mandas.docker.client.DockerClient.RemoveContainerParam.forceKill;
import static org.mandas.docker.client.DockerClient.Signal.SIGKILL;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.LogStream;
import org.mandas.docker.client.ProgressHandler;
import org.mandas.docker.client.builder.DockerClientBuilder.EntityProcessing;
import org.mandas.docker.client.builder.resteasy.ResteasyDockerClientBuilder;
import org.mandas.docker.client.exceptions.DockerCertificateException;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ContainerConfig;
import org.mandas.docker.client.messages.HostConfig;
import org.mandas.docker.client.messages.PortBinding;
import org.mandas.docker.client.messages.ProgressMessage;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.config.Config;

/**
 * Docker Service.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.1.2
 */
public class DockerService {

    final Logger log = getLogger(lookup().lookupClass());

    private Config config;
    private String dockerDefaultSocket;
    private int dockerWaitTimeoutSec;
    private int dockerPollTimeMs;
    private DockerClient dockerClient;
    private InternalPreferences preferences;
    private boolean localDaemon = true;

    public DockerService(Config config, InternalPreferences preferences) {
        this.config = config;
        this.preferences = preferences;

        dockerDefaultSocket = getConfig().getDockerDefaultSocket();
        dockerWaitTimeoutSec = getConfig().getDockerWaitTimeoutSec();
        dockerPollTimeMs = getConfig().getDockerPollTimeMs();

        String dockerServerUrl = getConfig().getDockerServerUrl();
        ResteasyDockerClientBuilder dockerClientBuilder = new ResteasyDockerClientBuilder();
        if (dockerServerUrl.isEmpty()) {
            try {
                dockerClientBuilder = dockerClientBuilder.fromEnv();
            } catch (DockerCertificateException e) {
                throw new SeleniumJupiterException(e);
            }
        } else {
            log.debug("Using Docker server URL {}", dockerServerUrl);
            dockerClientBuilder = dockerClientBuilder.uri(dockerServerUrl);
        }

        EntityProcessing requestEntityProcessing = EntityProcessing.CHUNKED;
        String dockerRequestEntityProcessing = config
                .getDockerRequestEntityProcessing();
        if (dockerRequestEntityProcessing.equalsIgnoreCase("BUFFERED")) {
            requestEntityProcessing = EntityProcessing.BUFFERED;
        }
        log.trace("Using RequestEntityProcessing {}", requestEntityProcessing);
        dockerClientBuilder.entityProcessing(requestEntityProcessing);

        dockerClient = dockerClientBuilder.build();
    }

    public String getHost(String containerId, String network)
            throws DockerException, InterruptedException {
        String dockerHost = getConfig().getDockerHost();
        if (!dockerHost.isEmpty()) {
            return dockerHost;
        }

        return IS_OS_LINUX
                ? dockerClient.inspectContainer(containerId).networkSettings()
                        .networks().get(network).gateway()
                : dockerClient.getHost();
    }

    public synchronized String startContainer(DockerContainer dockerContainer)
            throws DockerException, InterruptedException {
        String imageId = dockerContainer.getImageId();
        log.info("Starting Docker container {}", imageId);
        org.mandas.docker.client.messages.HostConfig.Builder hostConfigBuilder = HostConfig
                .builder();
        org.mandas.docker.client.messages.ContainerConfig.Builder containerConfigBuilder = ContainerConfig
                .builder();

        boolean privileged = dockerContainer.isPrivileged();
        if (privileged) {
            log.trace("Using privileged mode");
            hostConfigBuilder.privileged(true);
            hostConfigBuilder.capAdd("NET_ADMIN", "NET_RAW");
        }
        Optional<String> network = dockerContainer.getNetwork();
        if (network.isPresent()) {
            log.trace("Using network: {}", network.get());
            hostConfigBuilder.networkMode(network.get());
        }
        Optional<Map<String, List<PortBinding>>> portBindings = dockerContainer
                .getPortBindings();
        if (portBindings.isPresent()) {
            log.trace("Using port bindings: {}", portBindings.get());
            hostConfigBuilder.portBindings(portBindings.get());
            containerConfigBuilder.exposedPorts(portBindings.get().keySet());
        }
        Optional<List<String>> binds = dockerContainer.getBinds();
        if (binds.isPresent()) {
            log.trace("Using binds: {}", binds.get());
            hostConfigBuilder.binds(binds.get());
        }
        Optional<List<String>> envs = dockerContainer.getEnvs();
        if (envs.isPresent()) {
            log.trace("Using envs: {}", envs.get());
            containerConfigBuilder.env(envs.get());
        }
        Optional<List<String>> cmd = dockerContainer.getCmd();
        if (cmd.isPresent()) {
            log.trace("Using cmd: {}", cmd.get());
            containerConfigBuilder.cmd(cmd.get());
        }
        Optional<List<String>> entryPoint = dockerContainer.getEntryPoint();
        if (entryPoint.isPresent()) {
            log.trace("Using entryPoint: {}", entryPoint.get());
            containerConfigBuilder.entrypoint(entryPoint.get());
        }

        ContainerConfig createContainer = containerConfigBuilder.image(imageId)
                .hostConfig(hostConfigBuilder.build()).build();
        String containerId = dockerClient.createContainer(createContainer).id();
        dockerClient.startContainer(containerId);

        return containerId;
    }

    public String execCommandInContainer(String containerId, String... command)
            throws DockerException, InterruptedException {
        String commandStr = Arrays.toString(command);
        log.trace("Running command {} in container {}", commandStr,
                containerId);
        String execId = dockerClient.execCreate(containerId, command,
                DockerClient.ExecCreateParam.attachStdout(),
                DockerClient.ExecCreateParam.attachStderr()).id();
        String output = null;
        try (LogStream stream = dockerClient.execStart(execId)) {
            if (stream.hasNext()) {
                output = UTF_8.decode(stream.next().content()).toString();
            }
        } catch (Exception e) {
            log.trace("Exception executing command in container", e);
        }
        log.trace("Result of command {} in container {}: {}", commandStr,
                containerId, output);
        return output;
    }

    public String getBindPort(String containerId, String exposed)
            throws DockerException, InterruptedException {
        Map<String, List<PortBinding>> ports = dockerClient
                .inspectContainer(containerId).networkSettings().ports();
        List<PortBinding> exposedPort = ports.get(exposed);
        log.trace("Port list {} -- Exposed port {} = {}", ports, exposed,
                exposedPort);
        if (ports.isEmpty() || exposedPort.isEmpty()) {
            String dockerImage = dockerClient.inspectContainer(containerId)
                    .config().image();
            throw new SeleniumJupiterException("Port " + exposed
                    + " is not bindable in container " + dockerImage);
        }
        return exposedPort.get(0).hostPort();
    }

    public void pullImage(String imageId)
            throws DockerException, InterruptedException {
        if (!preferences.checkKeyInPreferences(imageId)
                || !getConfig().isUsePreferences() || !localDaemon) {
            log.info("Pulling Docker image {}", imageId);
            dockerClient.pull(imageId, new ProgressHandler() {
                @Override
                public void progress(ProgressMessage message)
                        throws DockerException {
                    log.trace("Pulling Docker image {} ... {}", imageId,
                            message);
                }
            });
            log.trace("Docker image {} downloaded", imageId);
            if (getConfig().isUsePreferences() && localDaemon) {
                preferences.putValueInPreferencesIfEmpty(imageId, "pulled");
            }
        }
    }

    public boolean existsImage(String imageId) {
        boolean exists = true;
        try {
            dockerClient.inspectImage(imageId);
            log.trace("Docker image {} already exists", imageId);

        } catch (Exception e) {
            log.trace("Image {} does not exist", imageId);
            exists = false;
        }
        return exists;
    }

    public synchronized void stopAndRemoveContainer(String containerId,
            String imageId) {
        log.info("Stopping Docker container {}", imageId);
        try {
            stopContainer(containerId);
            removeContainer(containerId);
        } catch (Exception e) {
            log.warn("Exception stopping container {}", imageId, e);
        }
    }

    public synchronized void stopContainer(String containerId)
            throws DockerException, InterruptedException {
        int stopTimeoutSec = getConfig().getDockerStopTimeoutSec();
        if (stopTimeoutSec == 0) {
            log.trace("Killing container {}", containerId);
            dockerClient.killContainer(containerId, SIGKILL);
        } else {
            log.trace("Stopping container {} (timeout {} seconds)", containerId,
                    stopTimeoutSec);
            dockerClient.stopContainer(containerId, stopTimeoutSec);
        }
    }

    public synchronized void removeContainer(String containerId)
            throws DockerException, InterruptedException {
        log.trace("Removing container {}", containerId);
        int stopTimeoutSec = getConfig().getDockerStopTimeoutSec();
        if (stopTimeoutSec == 0) {
            dockerClient.removeContainer(containerId, forceKill());
        } else {
            dockerClient.removeContainer(containerId);
        }
    }

    public String getDockerDefaultSocket() {
        return dockerDefaultSocket;
    }

    public int getDockerWaitTimeoutSec() {
        return dockerWaitTimeoutSec;
    }

    public int getDockerPollTimeMs() {
        return dockerPollTimeMs;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    public void close() {
        dockerClient.close();
    }

    public void updateDockerClient(String url) {
        if (localDaemon) {
            log.debug("Updating Docker client using URL {}", url);
            dockerClient = new ResteasyDockerClientBuilder().uri(url).build();
            localDaemon = false;
        }
    }

    public Config getConfig() {
        return config;
    }

}
