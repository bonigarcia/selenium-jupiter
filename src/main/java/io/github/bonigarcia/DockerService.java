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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.io.CharStreams;

/**
 * Docker Service.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.1.2
 */
public class DockerService {

    final Logger log = getLogger(lookup().lookupClass());

    private int dockerWaitTimeoutSec = getInt(
            "sel.jup.docker.wait.timeout.sec");
    private int dockerPollTimeMs = getInt("sel.jup.docker.poll.time.ms");
    private String dockerDefaultHostIp = getString(
            "sel.jup.docker.default.host");
    private String dockerDefaultSocket = getString(
            "sel.jup.docker.default.socket");

    private String dockerServerIp;
    private boolean runningInContainer = false;
    private boolean containerCheked = false;

    private DockerClient dockerClient;

    public DockerService() {
        dockerClient = DockerClientBuilder
                .getInstance("unix://" + dockerDefaultSocket).build();
    }

    public String getDockerServerIp() {
        try {
            if (dockerServerIp == null) {
                if (IS_OS_WINDOWS) {
                    dockerServerIp = getDockerMachineIp();
                } else {
                    if (!containerCheked) {
                        runningInContainer = isRunningInContainer();
                        containerCheked = true;
                    }
                    if (runningInContainer) {
                        dockerServerIp = getContainerIp();

                    } else {
                        dockerServerIp = dockerDefaultHostIp;
                    }
                }
                log.trace("Docker server IP: {}", dockerServerIp);
            }
        } catch (Exception e) {
            throw new SeleniumJupiterException(
                    "Exception geting Docker Server IP", e);
        }

        return dockerServerIp;
    }

    public String getContainerIp() throws IOException {
        String ipRoute = runAndWait("sh", "-c", "/sbin/ip route");
        String[] tokens = ipRoute.split("\\s");
        return tokens[2];
    }

    public String getDockerMachineIp() throws IOException {
        return runAndWait("docker-machine", "ip").replaceAll("\\r", "")
                .replaceAll("\\n", "");
    }

    public void startAndWaitContainer(DockerContainer dockerContainer) {
        String containerName = dockerContainer.getContainerName();
        String imageId = dockerContainer.getImageId();
        log.debug("Starting Docker container {}", imageId);

        if (!isRunningContainer(containerName)) {
            pullImageIfNecessary(imageId);

            try (CreateContainerCmd createContainer = dockerClient
                    .createContainerCmd(imageId).withName(containerName)) {

                Optional<String> network = dockerContainer.getNetwork();
                if (network.isPresent()) {
                    log.trace("Using network: {}", network.get());
                    createContainer.withNetworkMode(network.get());
                }
                Optional<List<PortBinding>> portBindings = dockerContainer
                        .getPortBindings();
                if (portBindings.isPresent()) {
                    log.trace("Using port binding: {}", portBindings.get());
                    createContainer.withPortBindings(portBindings.get());
                }
                Optional<List<Volume>> volumes = dockerContainer.getVolumes();
                if (volumes.isPresent()) {
                    log.trace("Using volumes: {}", volumes.get());
                    createContainer.withVolumes(volumes.get());
                }
                Optional<List<Bind>> binds = dockerContainer.getBinds();
                if (binds.isPresent()) {
                    log.trace("Using binds: {}", binds.get());
                    createContainer.withBinds(binds.get());
                }
                Optional<List<String>> envs = dockerContainer.getEnvs();
                if (envs.isPresent()) {
                    log.trace("Using envs: {}", envs.get());
                    createContainer.withEnv(envs.get());
                }

                createContainer.exec();
                dockerClient.startContainerCmd(containerName).exec();
                waitForContainer(containerName);
            }
        } else {
            log.warn("Container {} already running", containerName);
        }
    }

    public void pullImageIfNecessary(String imageId) {
        if (!existsImage(imageId)) {
            log.info("Pulling Docker image {} ... please wait", imageId);
            dockerClient.pullImageCmd(imageId)
                    .exec(new PullImageResultCallback()).awaitSuccess();
            log.trace("Docker image {} downloaded", imageId);
        }
    }

    public boolean existsImage(String imageId) {
        boolean exists = true;
        try {
            dockerClient.inspectImageCmd(imageId).exec();
            log.trace("Docker image {} already exists", imageId);

        } catch (NotFoundException e) {
            log.trace("Image {} does not exist", imageId);
            exists = false;
        }
        return exists;
    }

    public void stopAndRemoveContainer(String imageName, String containerName) {
        log.debug("Stopping Docker container {}", imageName);
        stopContainer(containerName);
        removeContainer(containerName);
    }

    public void stopContainer(String containerName) {
        if (isRunningContainer(containerName)) {
            log.trace("Stopping container {}", containerName);
            dockerClient.stopContainerCmd(containerName).exec();

        } else {
            log.trace("Container {} is not running", containerName);
        }
    }

    public void removeContainer(String containerName) {
        if (existsContainer(containerName)) {
            log.trace("Removing container {}", containerName);
            dockerClient.removeContainerCmd(containerName)
                    .withRemoveVolumes(true).exec();
        }
    }

    public String execCommand(String containerName, boolean awaitCompletion,
            String... command) throws IOException, InterruptedException {
        assert (command.length > 0);

        String output = null;
        String commandStr = Arrays.toString(command);

        log.debug("Executing command {} in container {} (await completion {})",
                commandStr, containerName, awaitCompletion);
        if (existsContainer(containerName)) {
            ExecCreateCmdResponse exec = dockerClient
                    .execCreateCmd(containerName).withCmd(command).withTty(true)
                    .withAttachStdin(true).withAttachStdout(true)
                    .withAttachStderr(true).exec();

            log.trace("Command executed. Exec id: {}", exec.getId());
            OutputStream outputStream = new ByteArrayOutputStream();
            try (ExecStartResultCallback startResultCallback = dockerClient
                    .execStartCmd(exec.getId()).withDetach(false).withTty(true)
                    .exec(new ExecStartResultCallback(outputStream,
                            outputStream))) {

                if (awaitCompletion) {
                    startResultCallback.awaitCompletion();
                }
                output = outputStream.toString();

            } finally {
                log.info("Callback terminated. Result: {}", output);
            }
        }
        return output;
    }

    public InputStream getFileFromContainer(String containerName,
            String fileName) {
        InputStream inputStream = null;
        if (existsContainer(containerName)) {
            log.debug("Copying {} from container {}", fileName, containerName);

            inputStream = dockerClient
                    .copyArchiveFromContainerCmd(containerName, fileName)
                    .exec();
        }
        return inputStream;
    }

    public void waitForContainer(String containerName) {
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

    public boolean isRunningContainer(String containerName) {
        boolean isRunning = false;
        if (existsContainer(containerName)) {
            isRunning = dockerClient.inspectContainerCmd(containerName).exec()
                    .getState().getRunning();
            log.trace("Container {} is running: {}", containerName, isRunning);
        }

        return isRunning;
    }

    public boolean existsContainer(String containerName) {
        boolean exists = true;
        try {
            dockerClient.inspectContainerCmd(containerName).exec();
            log.trace("Container {} already exist", containerName);

        } catch (NotFoundException e) {
            log.trace("Container {} does not exist", containerName);
            exists = false;
        }
        return exists;
    }

    public String generateContainerName(String prefix) {
        String randomSufix = new BigInteger(130, new SecureRandom())
                .toString(32);
        return prefix + randomSufix;
    }

    public int findRandomOpenPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            log.error("Exception finding free port", e);
            return 0;
        }
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
        return runAndWaitArray(command);
    }

    public String runAndWaitArray(String[] command) throws IOException {
        assert (command.length > 0);

        String commandStr = Arrays.toString(command);
        log.debug("Running command on the shell: {}", commandStr);
        String result = runAndWaitNoLog(command);
        log.trace("Result: {}", result);
        return result;
    }

    public String runAndWaitNoLog(String... command) throws IOException {
        assert (command.length > 0);

        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .start();
        String output = CharStreams.toString(
                new InputStreamReader(process.getInputStream(), UTF_8));
        process.destroy();
        return output;
    }

    public String getDockerDefaultSocket() {
        return dockerDefaultSocket;
    }

}
