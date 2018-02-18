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
package io.github.bonigarcia;

import static io.github.bonigarcia.SeleniumJupiter.getInt;
import static io.github.bonigarcia.SeleniumJupiter.getString;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DefaultDockerClient.Builder;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;

/**
 * Docker Service.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.1.2
 */
public class DockerService {

    final Logger log = getLogger(lookup().lookupClass());

    private String dockerServerHost;
    private boolean runningInContainer = false;
    private boolean containerCheked = false;
    private String dockerDefaultHost;
    private String dockerDefaultSocket;
    private int dockerWaitTimeoutSec;
    private int dockerPollTimeMs;

    private DockerClient dockerClient;

    public DockerService() throws DockerCertificateException {
        dockerDefaultHost = getString("sel.jup.docker.default.host");
        dockerDefaultSocket = getString("sel.jup.docker.default.socket");
        dockerWaitTimeoutSec = getInt("sel.jup.docker.wait.timeout.sec");
        dockerPollTimeMs = getInt("sel.jup.docker.poll.time.ms");

        Builder dockerClientBuilder = DefaultDockerClient.fromEnv();

        String dockerServerUrl = getString("sel.jup.docker.server.url");
        if (!dockerServerUrl.isEmpty()) {
            DefaultDockerClient.builder().uri(dockerServerUrl);
        }
        dockerClient = dockerClientBuilder.build();
    }

    public String getDockerServerHost() {
        try {
            if (dockerServerHost == null) {
                if (IS_OS_WINDOWS) {
                    dockerServerHost = getDockerMachineIp();
                } else {
                    if (!containerCheked) {
                        runningInContainer = isRunningInContainer();
                        containerCheked = true;
                    }
                    if (runningInContainer) {
                        dockerServerHost = getContainerIp();
                    } else {
                        dockerServerHost = getDockerDefaultHost();
                    }
                }
                log.trace("Docker server host: {}", dockerServerHost);
            }
        } catch (Exception e) {
            throw new SeleniumJupiterException(e);
        }

        return dockerServerHost;
    }

    public String getContainerIp() throws IOException {
        String ipRoute = runAndWait("sh", "-c", "/sbin/ip route");
        return ipRoute.split("\\s")[2];
    }

    public String getDockerMachineIp() throws IOException {
        return runAndWait("docker-machine", "ip").replaceAll("\\r", "")
                .replaceAll("\\n", "");
    }

    public String startAndWaitContainer(DockerContainer dockerContainer)
            throws DockerException, InterruptedException {
        String imageId = dockerContainer.getImageId();
        log.debug("Starting Docker container {}", imageId);
        pullImageIfNecessary(imageId);
        com.spotify.docker.client.messages.HostConfig.Builder hostConfigBuilder = HostConfig
                .builder();
        com.spotify.docker.client.messages.ContainerConfig.Builder containerConfigBuilder = ContainerConfig
                .builder();

        Optional<String> network = dockerContainer.getNetwork();
        if (network.isPresent()) {
            log.trace("Using network: {}", network.get());
            hostConfigBuilder.networkMode(network.get());
        }
        Optional<Map<String, List<PortBinding>>> portBindings = dockerContainer
                .getPortBindings();
        if (portBindings.isPresent()) {
            log.trace("Using port binding {}:{}", portBindings.get());
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

        ContainerCreation createContainer2 = dockerClient
                .createContainer(createContainer);
        String containerId = createContainer2.id();
        dockerClient.startContainer(containerId);

        return containerId;
    }

    public String getBindPort(String containerId, String exposed)
            throws DockerException, InterruptedException {
        ImmutableMap<String, List<PortBinding>> ports = dockerClient
                .inspectContainer(containerId).networkSettings().ports();
        return ports.get(exposed).get(0).hostPort();
    }

    public void pullImageIfNecessary(String imageId)
            throws DockerException, InterruptedException {
        if (!existsImage(imageId)) {
            log.info("Pulling Docker image {} ... please wait", imageId);
            dockerClient.pull(imageId);
            log.trace("Docker image {} downloaded", imageId);
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

    public void stopAndRemoveContainer(String containerName, String imageName)
            throws DockerException, InterruptedException {
        log.debug("Stopping Docker container {}", imageName);
        stopContainer(containerName);
        removeContainer(containerName);
    }

    public void stopContainer(String containerName)
            throws DockerException, InterruptedException {
        if (isRunningContainer(containerName)) {
            int stopTimeoutSec = getInt("sel.jup.docker.stop.timeout.sec");
            log.trace("Stopping container {} (timeout {} seconds)",
                    containerName, stopTimeoutSec);
            dockerClient.stopContainer(containerName, stopTimeoutSec);

        } else {
            log.trace("Container {} is not running", containerName);
        }
    }

    public void removeContainer(String containerName)
            throws DockerException, InterruptedException {
        if (existsContainer(containerName)) {
            log.trace("Removing container {}", containerName);
            dockerClient.removeContainer(containerName);
        }
    }

    public void waitForContainer(String containerName)
            throws DockerException, InterruptedException {
        boolean isRunning = false;
        long timeoutMs = currentTimeMillis()
                + SECONDS.toMillis(dockerWaitTimeoutSec);
        do {
            isRunning = isRunningContainer(containerName);
            if (!isRunning) {
                // Check timeout
                if (currentTimeMillis() > timeoutMs) {
                    throw new SeleniumJupiterException(
                            "Timeout of " + dockerWaitTimeoutSec
                                    + " seconds waiting for container "
                                    + containerName);
                }

                // Wait poll time
                log.trace("Container {} is not still running ... waiting {} ms",
                        containerName, dockerPollTimeMs);
                try {
                    sleep(dockerPollTimeMs);
                } catch (InterruptedException e) {
                    log.warn(
                            "Interrupted Exception while waiting for container",
                            e);
                    currentThread().interrupt();
                }
            }
        } while (!isRunning);
    }

    public boolean isRunningContainer(String containerId)
            throws DockerException, InterruptedException {
        boolean isRunning = false;
        if (existsContainer(containerId)) {
            isRunning = dockerClient.inspectContainer(containerId).state()
                    .running();
            log.trace("Container {} is running: {}", containerId, isRunning);
        }

        return isRunning;
    }

    public boolean existsContainer(String containerName) {
        boolean exists = true;
        try {
            dockerClient.inspectContainer(containerName);
            log.trace("Container {} already exist", containerName);

        } catch (Exception e) {
            log.trace("Container {} does not exist", containerName);
            exists = false;
        }
        return exists;
    }

    public boolean isRunningInContainer() {
        boolean isRunningInContainer = false;
        try (BufferedReader br = Files
                .newBufferedReader(Paths.get("/proc/1/cgroup"), UTF_8)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.contains("/docker")) {
                    return true;
                }
                isRunningInContainer = false;
            }

        } catch (IOException e) {
            log.debug("Not running inside a Docker container");
        }
        return isRunningInContainer;
    }

    public String runAndWait(String... command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .start();
        String result = CharStreams.toString(
                new InputStreamReader(process.getInputStream(), UTF_8));
        process.destroy();
        if (log.isTraceEnabled()) {
            log.trace("Running command on the shell: {} -- result: {}",
                    Arrays.toString(command), result);
        }
        return result;
    }

    public String getDockerDefaultHost() {
        return dockerDefaultHost;
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

}
