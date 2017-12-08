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
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

    private int dockerWaitTimeoutSec = getInt("sel.jup.docker.wait.timeout");
    private int dockerPollTimeMs = getInt("sel.jup.docker.poll.time");
    private String dockerDefaultHostIp = getString(
            "sel.jup.docker.default.host");
    private String dockerDefaultSocket = getString(
            "sel.jup.docker.default.socket");
    private String dockerServerUrl = getString("sel.jup.docker.server.url");

    private String dockerServerIp;
    private boolean runningInContainer = false;
    private boolean containerCheked = false;

    private DockerClient dockerClient;

    public DockerService() {
        dockerClient = DockerClientBuilder.getInstance(dockerServerUrl).build();
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
                log.info("Docker server IP: {}", dockerServerIp);
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

        if (!isRunningContainer(containerName)) {
            pullImageIfNecessary(imageId);

            try (CreateContainerCmd createContainer = dockerClient
                    .createContainerCmd(imageId).withName(containerName)) {

                Optional<String> network = dockerContainer.getNetwork();
                if (network.isPresent()) {
                    log.info("Using network: {}", network.get());
                    createContainer.withNetworkMode(network.get());
                }
                Optional<List<PortBinding>> portBindings = dockerContainer
                        .getPortBindings();
                if (portBindings.isPresent()) {
                    log.info("Using port binding: {}", portBindings.get());
                    createContainer.withPortBindings(portBindings.get());
                }
                Optional<List<Volume>> volumes = dockerContainer.getVolumes();
                if (volumes.isPresent()) {
                    log.info("Using volumes: {}", volumes.get());
                    createContainer.withVolumes(volumes.get());
                }
                Optional<List<Bind>> binds = dockerContainer.getBinds();
                if (binds.isPresent()) {
                    log.info("Using binds: {}", binds.get());
                    createContainer.withBinds(binds.get());
                }
                Optional<List<String>> envs = dockerContainer.getEnvs();
                if (envs.isPresent()) {
                    log.info("Using envs: {}", envs.get());
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
            log.info("Docker image {} downloaded", imageId);
        }
    }

    public boolean existsImage(String imageId) {
        boolean exists = true;
        try {
            dockerClient.inspectImageCmd(imageId).exec();
            log.info("Docker image {} already exists", imageId);

        } catch (NotFoundException e) {
            log.info("Image {} does not exist", imageId);
            exists = false;
        }
        return exists;
    }

    public void stopAndRemoveContainer(String containerName) {
        stopContainer(containerName);
        removeContainer(containerName);
    }

    public void stopContainer(String containerName) {
        if (isRunningContainer(containerName)) {
            log.info("Stopping container {}", containerName);
            dockerClient.stopContainerCmd(containerName).exec();

        } else {
            log.info("Container {} is not running", containerName);
        }
    }

    public void removeContainer(String containerName) {
        if (existsContainer(containerName)) {
            log.info("Removing container {}", containerName);
            dockerClient.removeContainerCmd(containerName)
                    .withRemoveVolumes(true).exec();
        }
    }

    public String execCommand(String containerName, boolean awaitCompletion,
            String... command) throws IOException, InterruptedException {
        assert (command.length > 0);

        String output = null;
        String commandStr = Arrays.toString(command);

        log.info("Executing command {} in container {} (await completion {})",
                commandStr, containerName, awaitCompletion);
        if (existsContainer(containerName)) {
            ExecCreateCmdResponse exec = dockerClient
                    .execCreateCmd(containerName).withCmd(command).withTty(true)
                    .withAttachStdin(true).withAttachStdout(true)
                    .withAttachStderr(true).exec();

            log.info("Command executed. Exec id: {}", exec.getId());
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
            log.info("Copying {} from container {}", fileName, containerName);

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
                log.info("Container {} is not still running ... waiting {} ms",
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
            log.info("Container {} is running: {}", containerName, isRunning);
        }

        return isRunning;
    }

    public boolean existsContainer(String containerName) {
        boolean exists = true;
        try {
            dockerClient.inspectContainerCmd(containerName).exec();
            log.info("Container {} already exist", containerName);

        } catch (NotFoundException e) {
            log.info("Container {} does not exist", containerName);
            exists = false;
        }
        return exists;
    }

    public void waitForHostIsReachable(String url) {
        long timeoutMillis = MILLISECONDS.convert(dockerWaitTimeoutSec,
                SECONDS);
        long endTimeMillis = System.currentTimeMillis() + timeoutMillis;

        log.info("Waiting for {} to be reachable (timeout {} seconds)", url,
                dockerWaitTimeoutSec);
        String errorMessage = "URL " + url + " not reachable in "
                + dockerWaitTimeoutSec + " seconds";
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[] {};
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs,
                            String authType) {
                        // No actions required
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs,
                            String authType) {
                        // No actions required
                    }
                } };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            waitUrl(url, timeoutMillis, endTimeMillis, errorMessage);

        } catch (Exception e) {
            // Not propagating multiple exceptions (NoSuchAlgorithmException,
            // KeyManagementException, IOException, InterruptedException) to
            // improve readability
            throw new SeleniumJupiterException(errorMessage, e);
        }

    }

    private void waitUrl(String url, long timeoutMillis, long endTimeMillis,
            String errorMessage) throws IOException, InterruptedException {
        int responseCode = 0;
        while (true) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url)
                        .openConnection();
                connection.setConnectTimeout((int) timeoutMillis);
                connection.setReadTimeout((int) timeoutMillis);
                connection.setRequestMethod("GET");
                responseCode = connection.getResponseCode();

                if (responseCode == HTTP_OK) {
                    log.info("URL already reachable");
                    break;
                } else {
                    log.info(
                            "URL {} not reachable (response {}). Trying again in {} ms",
                            url, responseCode, dockerPollTimeMs);
                }

            } catch (SSLHandshakeException | SocketException e) {
                log.info("Error {} waiting URL {}, trying again in {} ms",
                        e.getMessage(), url, dockerPollTimeMs);

            } finally {
                // Polling to wait a consistent state
                sleep(dockerPollTimeMs);
            }

            if (currentTimeMillis() > endTimeMillis) {
                throw new SeleniumJupiterException(errorMessage);
            }
        }
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
            log.info("Not running inside a Docker container");
        }
        return isRunningInContainer;
    }

    public String runAndWait(String... command) throws IOException {
        return runAndWaitArray(command);
    }

    public String runAndWaitArray(String[] command) throws IOException {
        assert (command.length > 0);

        String commandStr = Arrays.toString(command);
        log.info("Running command on the shell: {}", commandStr);
        String result = runAndWaitNoLog(command);
        log.info("Result: {}", result);
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
