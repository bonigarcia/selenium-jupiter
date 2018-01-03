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
package io.github.bonigarcia.handler;

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static io.github.bonigarcia.BrowserType.OPERA;
import static io.github.bonigarcia.SeleniumJupiter.getBoolean;
import static io.github.bonigarcia.SeleniumJupiter.getInt;
import static io.github.bonigarcia.SeleniumJupiter.getString;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;

import io.github.bonigarcia.AnnotationsReader;
import io.github.bonigarcia.BrowserType;
import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.DockerContainer;
import io.github.bonigarcia.DockerContainer.DockerBuilder;
import io.github.bonigarcia.DockerService;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.SelenoidConfig;

/**
 * Resolver for DockerDriver's.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class DockerDriverHandler {

    final Logger log = getLogger(lookup().lookupClass());

    DockerService dockerService;
    Map<String, String> containers;
    Path tmpDir;
    boolean recording;
    File recordingFile;
    String hostVideoFolder;
    SelenoidConfig selenoidConfig;

    public WebDriver resolve(DockerBrowser dockerBrowser, Parameter parameter,
            Optional<Object> testInstance,
            AnnotationsReader annotationsReader) {
        BrowserType browser = dockerBrowser.type();
        WebDriver webDriver = null;

        if (dockerService == null) {
            dockerService = new DockerService();
        }
        if (containers == null) {
            containers = new LinkedHashMap<>();
        }
        if (selenoidConfig == null) {
            selenoidConfig = new SelenoidConfig();
        }

        try {
            String version = dockerBrowser.version();

            boolean enableVnc = getBoolean("sel.jup.docker.vnc");
            recording = getBoolean("sel.jup.docker.recording");
            int selenoidPort = startDockerBrowser(browser, version, recording);
            int novncPort = 0;
            if (enableVnc) {
                novncPort = startDockerNoVnc();
            }

            DesiredCapabilities capabilities = browser.getCapabilities();

            if (!version.isEmpty()) {
                capabilities.setCapability("version",
                        selenoidConfig.getImageVersion(browser, version));
            }

            if (enableVnc) {
                capabilities.setCapability("enableVNC", true);
                capabilities.setCapability("screenResolution",
                        getString("sel.jup.docker.vnc.screen.resolution"));
            }

            if (recording) {
                capabilities.setCapability("enableVideo", true);
                capabilities.setCapability("videoScreenSize", getString(
                        "sel.jup.docker.recording.video.screen.size"));
                capabilities.setCapability("videoFrameRate",
                        getInt("sel.jup.docker.recording.video.frame.rate"));
            }

            Optional<Capabilities> optionalCapabilities = annotationsReader
                    .getCapabilities(parameter, testInstance);
            MutableCapabilities options = browser.getDriverHandler()
                    .getOptions(parameter, testInstance);

            // Due to bug in operablink the binary path must be set
            if (browser == OPERA) {
                ((OperaOptions) options).setBinary("/usr/bin/opera");
            }

            if (optionalCapabilities.isPresent()) {
                options.merge(optionalCapabilities.get());
            }

            capabilities.setCapability(browser.getOptionsKey(), options);

            log.trace("Using capabilities for Docker browser {}", capabilities);

            String dockerServerIp = dockerService.getDockerServerIp();
            String selenoidHubUrl = format("http://%s:%d/wd/hub",
                    dockerServerIp, selenoidPort);
            webDriver = new RemoteWebDriver(new URL(selenoidHubUrl),
                    capabilities);
            SessionId sessionId = ((RemoteWebDriver) webDriver).getSessionId();
            if (enableVnc) {
                String vncUrl = format(
                        "http://%s:%d/vnc.html?host=%s&port=%d&path=vnc/%s&resize=scale&autoconnect=true&password=%s",
                        dockerServerIp, novncPort, dockerServerIp, selenoidPort,
                        sessionId, getString("sel.jup.selenoid.vnc.password"));
                log.debug("Session {} VNC URL: {}", sessionId, vncUrl);
            }
            if (recording) {
                recordingFile = new File(hostVideoFolder, sessionId + ".mp4");
            }

            return webDriver;

        } catch (Exception e) {
            throw new SeleniumJupiterException(e);
        }

    }

    public void cleanup() {
        try {
            if (containers != null && dockerService != null) {
                int numContainers = containers.size();
                ExecutorService executorService = newFixedThreadPool(
                        getRuntime().availableProcessors());
                CountDownLatch latch = new CountDownLatch(numContainers);
                for (Map.Entry<String, String> entry : containers.entrySet()) {
                    executorService.submit(() -> {
                        try {
                            if (recording && entry.getValue().equals(
                                    getString("sel.jup.selenoid.image"))) {
                                waitForRecording();
                            }
                            dockerService.stopAndRemoveContainer(entry.getKey(),
                                    entry.getValue());
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                containers.clear();
                latch.await();
                executorService.shutdown();
            }
            if (tmpDir != null) {
                deleteDirectory(tmpDir.toFile());
            }
        } catch (Exception e) {
            log.warn("Exception cleaning DockerDriverHandler {}", e);
        }

    }

    private int startDockerNoVnc() {
        String novncImage = getString("sel.jup.novnc.image");
        dockerService.pullImageIfNecessary(novncImage);

        int novncPort = dockerService.findRandomOpenPort();
        Binding novncBindPort = bindPort(novncPort);
        ExposedPort novncExposedPort = tcp(getInt("sel.jup.novnc.port"));
        List<PortBinding> portBindings = asList(
                new PortBinding(novncBindPort, novncExposedPort));
        String novncContainerName = dockerService
                .generateContainerName("novnc");
        DockerContainer novncContainer = DockerContainer
                .dockerBuilder(novncImage, novncContainerName)
                .portBindings(portBindings).build();
        dockerService.startAndWaitContainer(novncContainer);
        containers.put(novncContainerName, novncImage);

        return novncPort;
    }

    private int startDockerBrowser(BrowserType browser, String version,
            boolean recording) throws IOException {
        String selenoidImage = getString("sel.jup.selenoid.image");
        String recordingImage = getString("sel.jup.docker.recording.image");
        String browserImage = !version.isEmpty()
                ? selenoidConfig.getImageFromVersion(browser, version)
                : selenoidConfig.getLatestImage(browser);

        dockerService.pullImageIfNecessary(selenoidImage);
        dockerService.pullImageIfNecessary(browserImage);
        if (recording) {
            dockerService.pullImageIfNecessary(recordingImage);
            hostVideoFolder = new File(
                    getString("sel.jup.docker.recording.folder"))
                            .getAbsolutePath();
        }

        String defaultSocket = dockerService.getDockerDefaultSocket();
        Volume defaultSocketVolume = new Volume(defaultSocket);
        Volume selenoidConfigVolume = new Volume("/etc/selenoid");

        List<Volume> volumes = new ArrayList<>();
        volumes.add(defaultSocketVolume);
        volumes.add(selenoidConfigVolume);
        String selenoidVideoFolder = "/opt/selenoid/video";
        Volume selenoidVideoVolume = new Volume(selenoidVideoFolder);
        tmpDir = createTempDirectory("");
        String browsersJson = selenoidConfig.getBrowsersJsonAsString();
        writeStringToFile(new File(tmpDir.toFile(), "browsers.json"),
                browsersJson, defaultCharset());
        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(defaultSocket, defaultSocketVolume));
        binds.add(new Bind(tmpDir.toFile().toString(), selenoidConfigVolume));
        if (recording) {
            binds.add(new Bind(hostVideoFolder, selenoidVideoVolume));
        }

        int selenoidPort = dockerService.findRandomOpenPort();
        Binding selenoidBindPort = bindPort(selenoidPort);
        int internalBrowserPort = getInt("sel.jup.selenoid.port");
        ExposedPort selenoidExposedPort = tcp(internalBrowserPort);
        List<PortBinding> portBindings = asList(
                new PortBinding(selenoidBindPort, selenoidExposedPort));
        String selenoidContainerName = dockerService
                .generateContainerName("selenoid");

        String browserTimeout = getString(
                "sel.jup.docker.browser.timeout.duration");
        List<String> cmd = asList("-listen", ":" + internalBrowserPort, "-conf",
                "/etc/selenoid/browsers.json", "-video-output-dir",
                "/opt/selenoid/video/", "-timeout", browserTimeout);

        DockerBuilder dockerBuilder = DockerContainer
                .dockerBuilder(selenoidImage, selenoidContainerName)
                .portBindings(portBindings).volumes(volumes).binds(binds)
                .cmd(cmd);
        if (recording) {
            List<String> envs = asList(
                    "OVERRIDE_VIDEO_OUTPUT_DIR=" + hostVideoFolder);
            dockerBuilder.envs(envs);
        }

        DockerContainer selenoidContainer = dockerBuilder.build();
        dockerService.startAndWaitContainer(selenoidContainer);
        containers.put(selenoidContainerName, selenoidImage);

        return selenoidPort;
    }

    private void waitForRecording() {
        int dockerWaitTimeoutSec = getInt("sel.jup.docker.wait.timeout.sec");
        int dockerPollTimeMs = getInt("sel.jup.docker.poll.time.ms");
        long timeoutMs = currentTimeMillis()
                + SECONDS.toMillis(dockerWaitTimeoutSec);
        log.debug("Waiting for recording {} to be available", recordingFile);
        while (!recordingFile.exists()) {
            if (currentTimeMillis() > timeoutMs) {
                log.warn("Timeout of {} seconds waiting for file {}",
                        dockerWaitTimeoutSec, recordingFile);
                break;
            }
            log.trace("Recording {} not present ... waiting {} ms",
                    recordingFile, dockerPollTimeMs);
            try {
                sleep(dockerPollTimeMs);
            } catch (InterruptedException e) {
                log.warn("Interrupted Exception while waiting for container",
                        e);
                currentThread().interrupt();
            }
        }
    }

}
