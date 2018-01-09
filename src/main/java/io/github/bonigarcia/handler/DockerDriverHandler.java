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
import static io.github.bonigarcia.SeleniumJupiter.getOutputFolder;
import static io.github.bonigarcia.SeleniumJupiter.getString;
import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.extension.ExtensionContext;
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
    File tmpDir;
    boolean recording;
    File recordingFile;
    File hostVideoFolder;
    SelenoidConfig selenoidConfig;
    ExtensionContext context;
    Parameter parameter;
    Optional<Object> testInstance;
    AnnotationsReader annotationsReader;

    public DockerDriverHandler(ExtensionContext context, Parameter parameter,
            Optional<Object> testInstance,
            AnnotationsReader annotationsReader) {
        this.context = context;
        this.parameter = parameter;
        this.testInstance = testInstance;
        this.annotationsReader = annotationsReader;
    }

    public WebDriver resolve(DockerBrowser dockerBrowser) {
        BrowserType browser = dockerBrowser.type();
        String version = dockerBrowser.version();
        return resolve(browser, version);
    }

    public WebDriver resolve(BrowserType browser, String version) {
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
            boolean enableVnc = getBoolean("sel.jup.vnc");
            recording = getBoolean("sel.jup.recording");
            int selenoidPort = startDockerBrowser(browser, version, recording);
            int novncPort = 0;
            if (enableVnc) {
                novncPort = startDockerNoVnc();
            }

            DesiredCapabilities capabilities = browser.getCapabilities();

            if (version != null && !version.isEmpty()) {
                capabilities.setCapability("version",
                        selenoidConfig.getImageVersion(browser, version));
            }

            if (enableVnc) {
                capabilities.setCapability("enableVNC", true);
                capabilities.setCapability("screenResolution",
                        getString("sel.jup.vnc.screen.resolution"));
            }

            if (recording) {
                capabilities.setCapability("enableVideo", true);
                capabilities.setCapability("videoScreenSize",
                        getString("sel.jup.recording.video.screen.size"));
                capabilities.setCapability("videoFrameRate",
                        getInt("sel.jup.recording.video.frame.rate"));
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

            String dockerServerIp = dockerService.getDockerServerHost();
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

                if (getBoolean("sel.jup.vnc.create.redirect.html.page")) {
                    String outputFolder = getOutputFolder(context);
                    String vncHtmlPage = format(
                            "<html><body onload=\"window.location.href='%s'\"></html>",
                            vncUrl);
                    Files.write(Paths.get(outputFolder, sessionId + ".html"),
                            vncHtmlPage.getBytes());
                }
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
                if (numContainers > 0) {
                    ExecutorService executorService = newFixedThreadPool(
                            numContainers);
                    CountDownLatch latch = new CountDownLatch(numContainers);
                    for (Map.Entry<String, String> entry : containers
                            .entrySet()) {
                        executorService.submit(() -> {
                            try {
                                if (recording && entry.getValue().equals(
                                        getString("sel.jup.selenoid.image"))) {
                                    waitForRecording();
                                }
                                dockerService.stopAndRemoveContainer(
                                        entry.getKey(), entry.getValue());
                            } finally {
                                latch.countDown();
                            }
                        });
                    }
                    containers.clear();
                    latch.await();
                    executorService.shutdown();
                }
            }
            if (tmpDir != null) {
                deleteDirectory(tmpDir);
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
        String recordingImage = getString("sel.jup.recording.image");

        String browserImage;
        if (version == null || version.isEmpty()) {
            log.debug("Using {} version {} (latest)", browser,
                    selenoidConfig.getDefaultBrowser(browser));
            browserImage = selenoidConfig.getLatestImage(browser);
        } else {
            log.debug("Using {} version {}", browser, version);
            browserImage = selenoidConfig.getImageFromVersion(browser, version);
        }

        dockerService.pullImageIfNecessary(selenoidImage);
        dockerService.pullImageIfNecessary(browserImage);
        if (recording) {
            dockerService.pullImageIfNecessary(recordingImage);
            hostVideoFolder = new File(getOutputFolder(context));
        }

        tmpDir = new File(getProperty("java.io.tmpdir"),
                randomUUID().toString());
        tmpDir.mkdirs();
        String browsersJson = selenoidConfig.getBrowsersJsonAsString();
        writeStringToFile(new File(tmpDir, "browsers.json"), browsersJson,
                defaultCharset());

        // volumes
        String defaultSocket = dockerService.getDockerDefaultSocket();
        Volume defaultSocketVolume = new Volume(defaultSocket);
        Volume selenoidConfigVolume = new Volume("/etc/selenoid");
        Volume selenoidVideoVolume = new Volume("/opt/selenoid/video");

        List<Volume> volumes = new ArrayList<>();
        volumes.add(defaultSocketVolume);
        volumes.add(selenoidConfigVolume);
        if (recording) {
            volumes.add(selenoidVideoVolume);
        }

        // binds
        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(defaultSocket, defaultSocketVolume));
        binds.add(new Bind(getDockerPath(tmpDir), selenoidConfigVolume));
        if (recording) {
            binds.add(new Bind(getDockerPath(hostVideoFolder),
                    selenoidVideoVolume));
        }

        // portBindings
        int selenoidPort = dockerService.findRandomOpenPort();
        Binding selenoidBindPort = bindPort(selenoidPort);
        int internalBrowserPort = getInt("sel.jup.selenoid.port");
        ExposedPort selenoidExposedPort = tcp(internalBrowserPort);
        List<PortBinding> portBindings = asList(
                new PortBinding(selenoidBindPort, selenoidExposedPort));
        String selenoidContainerName = dockerService
                .generateContainerName("selenoid");

        // cmd
        String browserTimeout = getString(
                "sel.jup.browser.session.timeout.duration");
        List<String> cmd = asList("-listen", ":" + internalBrowserPort, "-conf",
                "/etc/selenoid/browsers.json", "-video-output-dir",
                "/opt/selenoid/video/", "-timeout", browserTimeout);

        DockerBuilder dockerBuilder = DockerContainer
                .dockerBuilder(selenoidImage, selenoidContainerName)
                .portBindings(portBindings).volumes(volumes).binds(binds)
                .cmd(cmd);
        if (recording) {
            List<String> envs = asList("OVERRIDE_VIDEO_OUTPUT_DIR="
                    + getDockerPath(hostVideoFolder));
            dockerBuilder.envs(envs);
        }

        DockerContainer selenoidContainer = dockerBuilder.build();
        dockerService.startAndWaitContainer(selenoidContainer);
        containers.put(selenoidContainerName, selenoidImage);

        return selenoidPort;
    }

    private String getDockerPath(File file) {
        String fileString = file.getAbsolutePath();
        if (fileString.contains(":")) { // Windows
            fileString = toLowerCase(fileString.charAt(0))
                    + fileString.substring(1);
            fileString = fileString.replaceAll("\\\\", "/");
            fileString = fileString.replaceAll(":", "");
            fileString = "/" + fileString;
        }
        log.trace("The path of file {} in Docker format is {}", file,
                fileString);
        return fileString;
    }

    private void waitForRecording() {
        int dockerWaitTimeoutSec = dockerService.getDockerWaitTimeoutSec();
        int dockerPollTimeMs = dockerService.getDockerPollTimeMs();
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
