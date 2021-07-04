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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient.Builder;

import io.github.bonigarcia.seljup.config.Config;

/**
 * Docker Service.
 *
 * @author Boni Garcia
 * @since 1.1.2
 */
public class DockerService {

    final Logger log = getLogger(lookup().lookupClass());

    private Config config;
    private String dockerDefaultSocket;
    private int dockerWaitTimeoutSec;
    private int dockerPollTimeMs;
    private DockerClient dockerClient;
    private DockerCache dockerCache;
    private boolean localDaemon = true;
    private URI dockerHostUri;

    public DockerService(Config config, DockerCache dockerCache) {
        this.config = config;
        this.dockerCache = dockerCache;

        dockerDefaultSocket = getConfig().getDockerDefaultSocket();
        dockerWaitTimeoutSec = getConfig().getDockerWaitTimeoutSec();
        dockerPollTimeMs = getConfig().getDockerPollTimeMs();

        DockerHost dockerHostFromEnv = DockerHost.fromEnv();
        dockerClient = getDockerClient(dockerHostFromEnv.endpoint());
    }

    private DockerClient getDockerClient(String dockerHost) {
        DefaultDockerClientConfig.Builder dockerClientConfigBuilder = DefaultDockerClientConfig
                .createDefaultConfigBuilder();
        if (!isNullOrEmpty(dockerHost)) {
            dockerClientConfigBuilder.withDockerHost(dockerHost);
        }
        DockerClientConfig dockerClientConfig = dockerClientConfigBuilder
                .build();
        dockerHostUri = dockerClientConfig.getDockerHost();
        ApacheDockerHttpClient dockerHttpClient = new Builder()
                .dockerHost(dockerHostUri).build();

        return DockerClientBuilder.getInstance(dockerClientConfig)
                .withDockerHttpClient(dockerHttpClient).build();
    }

    public String getHost(String containerId, String network)
            throws DockerException {
        String dockerHost = getConfig().getDockerHost();
        if (!dockerHost.isEmpty()) {
            return dockerHost;
        }

        return IS_OS_LINUX
                ? dockerClient.inspectContainerCmd(containerId).exec()
                        .getNetworkSettings().getNetworks().get(network)
                        .getGateway()
                : Optional.ofNullable(dockerHostUri.getHost())
                        .orElse("localhost");
    }

    public synchronized String startContainer(DockerContainer dockerContainer)
            throws DockerException {
        String imageId = dockerContainer.getImageId();
        log.info("Starting Docker container {}", imageId);
        HostConfig hostConfigBuilder = new HostConfig();
        String containerId = null;

        try (CreateContainerCmd containerConfigBuilder = dockerClient
                .createContainerCmd(imageId)) {

            boolean privileged = dockerContainer.isPrivileged();
            if (privileged) {
                log.trace("Using privileged mode");
                hostConfigBuilder.withPrivileged(true);
                hostConfigBuilder.withCapAdd(Capability.NET_ADMIN,
                        Capability.NET_RAW);
            }
            Optional<String> network = dockerContainer.getNetwork();
            if (network.isPresent()) {
                log.trace("Using network: {}", network.get());
                hostConfigBuilder.withNetworkMode(network.get());
            }
            List<String> exposedPorts = dockerContainer.getExposedPorts();
            if (!exposedPorts.isEmpty()) {
                log.trace("Using exposed ports: {}", exposedPorts);
                containerConfigBuilder.withExposedPorts(exposedPorts.stream()
                        .map(ExposedPort::parse).collect(Collectors.toList()));
                hostConfigBuilder.withPublishAllPorts(true);
            }
            Optional<List<Bind>> binds = dockerContainer.getBinds();
            if (binds.isPresent()) {
                log.trace("Using binds: {}", binds.get());
                hostConfigBuilder.withBinds(binds.get());
            }
            Optional<List<String>> envs = dockerContainer.getEnvs();
            if (envs.isPresent()) {
                log.trace("Using envs: {}", envs.get());
                containerConfigBuilder
                        .withEnv(envs.get().toArray(new String[] {}));
            }
            Optional<List<String>> cmd = dockerContainer.getCmd();
            if (cmd.isPresent()) {
                log.trace("Using cmd: {}", cmd.get());
                containerConfigBuilder
                        .withCmd(cmd.get().toArray(new String[] {}));
            }
            Optional<List<String>> entryPoint = dockerContainer.getEntryPoint();
            if (entryPoint.isPresent()) {
                log.trace("Using entryPoint: {}", entryPoint.get());
                containerConfigBuilder.withEntrypoint(
                        entryPoint.get().toArray(new String[] {}));
            }

            containerId = containerConfigBuilder
                    .withHostConfig(hostConfigBuilder).exec().getId();
            dockerClient.startContainerCmd(containerId).exec();
        }

        return containerId;
    }

    public String execCommandInContainer(String containerId, String... command)
            throws DockerException {
        String commandStr = Arrays.toString(command);
        log.trace("Running command {} in container {}", commandStr,
                containerId);
        String execId = dockerClient.execCreateCmd(containerId).withCmd(command)
                .withAttachStdout(true).withAttachStderr(true).exec().getId();
        final StringBuilder output = new StringBuilder();
        dockerClient.execStartCmd(execId).exec(new Adapter<Frame>() {
            @Override
            public void onNext(Frame object) {
                output.append(new String(object.getPayload(), UTF_8));
                super.onNext(object);
            }
        });
        log.trace("Result of command {} in container {}: {}", commandStr,
                containerId, output);
        return output.toString();
    }

    public String getBindPort(String containerId, String exposed)
            throws DockerException {
        Ports ports = dockerClient.inspectContainerCmd(containerId).exec()
                .getNetworkSettings().getPorts();
        Binding[] exposedPort = ports.getBindings()
                .get(ExposedPort.parse(exposed));
        log.trace("Port list {} -- Exposed port {} = {}", ports, exposed,
                exposedPort);
        if (ports.getBindings().isEmpty() || exposedPort.length == 0) {
            String dockerImage = dockerClient.inspectContainerCmd(containerId)
                    .exec().getConfig().getImage();
            throw new SeleniumJupiterException("Port " + exposed
                    + " is not bindable in container " + dockerImage);
        }
        return exposedPort[0].getHostPortSpec();
    }

    public void pullImage(String imageId) throws DockerException {
        if (!dockerCache.checkKeyInDockerCache(imageId)
                || !getConfig().isUseDockerCache() || !localDaemon) {
            try {
                log.info("Pulling Docker image {}", imageId);
                dockerClient.pullImageCmd(imageId)
                        .exec(new Adapter<PullResponseItem>() {
                        }).awaitCompletion();
                log.trace("Docker image {} pulled", imageId);
                if (getConfig().isUseDockerCache() && localDaemon) {
                    dockerCache.putValueInDockerCacheIfEmpty(imageId, "pulled");
                }
            } catch (Exception e) {
                log.warn("Exception pulling image {}: {}", imageId,
                        e.getMessage());
            }
        }
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
            throws DockerException {
        int stopTimeoutSec = getConfig().getDockerStopTimeoutSec();
        if (stopTimeoutSec == 0) {
            log.trace("Killing container {}", containerId);
            dockerClient.killContainerCmd(containerId).exec();
        } else {
            log.trace("Stopping container {} (timeout {} seconds)", containerId,
                    stopTimeoutSec);
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(stopTimeoutSec).exec();
        }
    }

    public synchronized void removeContainer(String containerId)
            throws DockerException {
        log.trace("Removing container {}", containerId);
        int stopTimeoutSec = getConfig().getDockerStopTimeoutSec();
        if (stopTimeoutSec == 0) {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } else {
            dockerClient.removeContainerCmd(containerId).exec();
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

    public void close() throws IOException {
        dockerClient.close();
    }

    public void updateDockerClient(String url) {
        if (localDaemon) {
            log.debug("Updating Docker client using URL {}", url);
            dockerClient = getDockerClient(url);
            localDaemon = false;
        }
    }

    public Config getConfig() {
        return config;
    }

}
