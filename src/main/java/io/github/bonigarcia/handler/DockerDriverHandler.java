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

import static com.spotify.docker.client.messages.PortBinding.randomPort;
import static io.github.bonigarcia.BrowserType.OPERA;
import static io.github.bonigarcia.SeleniumJupiter.getBoolean;
import static io.github.bonigarcia.SeleniumJupiter.getInt;
import static io.github.bonigarcia.SeleniumJupiter.getOutputFolder;
import static io.github.bonigarcia.SeleniumJupiter.getString;
import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.file.Files.move;
import static java.nio.file.Files.write;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.PortBinding;

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
    File recordingFile;
    String name;
    boolean recording;
    File hostVideoFolder;
    SelenoidConfig selenoidConfig;
    ExtensionContext context;
    Parameter parameter;
    Optional<Object> testInstance;
    AnnotationsReader annotationsReader;
    String index;

    public DockerDriverHandler(ExtensionContext context, Parameter parameter,
            Optional<Object> testInstance, AnnotationsReader annotationsReader)
            throws DockerCertificateException {
        this.context = context;
        this.parameter = parameter;
        this.testInstance = testInstance;
        this.annotationsReader = annotationsReader;
        this.dockerService = new DockerService();
        this.containers = new LinkedHashMap<>();
        this.selenoidConfig = new SelenoidConfig();
    }

    public DockerDriverHandler(ExtensionContext context, Parameter parameter,
            Optional<Object> testInstance, AnnotationsReader annotationsReader,
            String index) throws DockerCertificateException {
        this(context, parameter, testInstance, annotationsReader);
        this.index = index;
    }

    public WebDriver resolve(DockerBrowser dockerBrowser) {
        BrowserType browser = dockerBrowser.type();
        String version = dockerBrowser.version();
        return resolve(browser, version);
    }

    public WebDriver resolve(BrowserType browser, String version) {
        try {
            boolean enableVnc = getBoolean("sel.jup.vnc");
            recording = getBoolean("sel.jup.recording");
            DesiredCapabilities capabilities = getCapabilities(browser,
                    enableVnc);

            String imageVersion;
            if (version != null && !version.isEmpty()
                    && !version.equalsIgnoreCase("latest")) {
                if (version.startsWith("latest-")) {
                    version = selenoidConfig.getVersionFromLabel(browser,
                            version);
                }
                imageVersion = selenoidConfig.getImageVersion(browser, version);
                capabilities.setCapability("version", imageVersion);
            } else {
                imageVersion = selenoidConfig.getDefaultBrowser(browser);
            }

            String selenoidHub = startDockerBrowser(browser, version,
                    recording);
            URL selenoidHubUrl = new URL(selenoidHub);
            String selenoidHost = selenoidHubUrl.getHost();
            int selenoidPort = selenoidHubUrl.getPort();

            log.debug("Using URL for hub {}", selenoidHub);
            WebDriver webdriver = new RemoteWebDriver(new URL(selenoidHub),
                    capabilities);
            SessionId sessionId = ((RemoteWebDriver) webdriver).getSessionId();

            String parameterName = parameter.getName();

            name = parameterName + "_" + browser + "_" + imageVersion + "_"
                    + ((RemoteWebDriver) webdriver).getSessionId();
            Optional<Method> testMethod = context.getTestMethod();
            if (testMethod.isPresent()) {
                name = testMethod.get().getName() + "_" + name;
            }
            if (index != null) {
                name += index;
            }

            if (enableVnc) {
                String novncUrl = startDockerNoVnc(selenoidHost, selenoidPort,
                        sessionId.toString(),
                        getString("sel.jup.selenoid.vnc.password"));
                log.debug("Session {} VNC URL: {}", sessionId, novncUrl);

                if (getBoolean("sel.jup.vnc.create.redirect.html.page")) {
                    String outputFolder = getOutputFolder(context);
                    String vncHtmlPage = format("<!DOCTYPE html>\n" + "<html>\n"
                            + "<head>\n"
                            + "<meta http-equiv=\"refresh\" content=\"0; url=%s\">\n"
                            + "</head>\n" + "<body>\n" + "</body>\n"
                            + "</html>", novncUrl);
                    write(Paths.get(outputFolder, name + ".html"),
                            vncHtmlPage.getBytes());
                }
            }

            if (recording) {
                recordingFile = new File(hostVideoFolder, sessionId + ".mp4");
            }

            return webdriver;

        } catch (Exception e) {
            log.error("Exception resolving {} ({} {})", parameter, browser,
                    version, e);
            throw new SeleniumJupiterException(e);
        }

    }

    private DesiredCapabilities getCapabilities(BrowserType browser,
            boolean enableVnc) throws IllegalAccessException, IOException {
        DesiredCapabilities capabilities = browser.getCapabilities();
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
        log.debug("Using capabilities for Docker browser {}", capabilities);
        return capabilities;
    }

    public String getName() {
        return name;
    }

    public void cleanup() {
        try {
            // Wait for recordings
            if (recording) {
                waitForRecording();
            }

            // Stop containers
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
                                try {
                                    dockerService.stopAndRemoveContainer(
                                            entry.getKey(), entry.getValue());
                                } catch (Exception e) {
                                    log.warn("Exception stopping container", e);
                                }
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
        } catch (Exception e) {
            log.warn("Exception cleaning DockerDriverHandler {}", e);
        }

    }

    private String startDockerBrowser(BrowserType browser, String version,
            boolean recording) throws DockerException, InterruptedException {
        String selenoidImage = getString("sel.jup.selenoid.image");
        String recordingImage = getString("sel.jup.recording.image");
        String network = getString("sel.jup.docker.network");

        String browserImage;
        if (version == null || version.isEmpty()
                || version.equalsIgnoreCase("latest")) {
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

        // portBindings
        Map<String, List<PortBinding>> portBindings = new HashMap<>();
        String defaultSelenoidPort = getString("sel.jup.selenoid.port");
        String internalSelenoidPort = defaultSelenoidPort;
        portBindings.put(internalSelenoidPort, asList(randomPort("0.0.0.0")));

        // binds
        String defaultSocket = dockerService.getDockerDefaultSocket();
        List<String> binds = new ArrayList<>();
        binds.add(defaultSocket + ":" + defaultSocket);
        if (recording) {
            binds.add(getDockerPath(hostVideoFolder) + ":/opt/selenoid/video");
        }

        // entrypoint & cmd
        List<String> entryPoint = asList("");
        int internalBrowserPort = getInt("sel.jup.selenoid.port");
        String browsersJson = selenoidConfig.getBrowsersJsonAsString();
        String browserTimeout = getString(
                "sel.jup.browser.session.timeout.duration");
        List<String> cmd = asList("sh", "-c", "mkdir -p /etc/selenoid/; echo '"
                + browsersJson + "' > /etc/selenoid/browsers.json; "
                + "/usr/bin/selenoid -listen :" + internalBrowserPort
                + " -conf /etc/selenoid/browsers.json -video-output-dir /opt/selenoid/video/ -timeout "
                + browserTimeout + " -container-network " + network);

        // envs
        List<String> envs = new ArrayList<>();
        envs.add("DOCKER_API_VERSION="
                + getString("sel.jup.docker.api.version"));

        if (recording) {
            envs.add("OVERRIDE_VIDEO_OUTPUT_DIR="
                    + getDockerPath(hostVideoFolder));
        }

        DockerBuilder dockerBuilder = DockerContainer
                .dockerBuilder(selenoidImage).portBindings(portBindings)
                .binds(binds).cmd(cmd).entryPoint(entryPoint).envs(envs)
                .network(network);

        DockerContainer selenoidContainer = dockerBuilder.build();
        String containerId = dockerService
                .startContainer(selenoidContainer);
        containers.put(containerId, selenoidImage);

        String selenoidHost = dockerService.getDockerGateway(containerId,
                network);
        String selenoidPort = dockerService.getBindPort(containerId,
                internalSelenoidPort + "/tcp");
        return format("http://%s:%s/wd/hub", selenoidHost, selenoidPort);
    }

    private String startDockerNoVnc(String selenoidHost, int selenoidPort,
            String sessionId, String novncPassword)
            throws DockerException, InterruptedException {
        String novncImage = getString("sel.jup.novnc.image");
        dockerService.pullImageIfNecessary(novncImage);

        Map<String, List<PortBinding>> portBindings = new HashMap<>();
        String defaultNovncPort = getString("sel.jup.novnc.port");
        portBindings.put(defaultNovncPort, asList(randomPort("0.0.0.0")));

        String network = getString("sel.jup.docker.network");
        DockerContainer novncContainer = DockerContainer
                .dockerBuilder(novncImage).portBindings(portBindings)
                .network(network).build();
        String containerId = dockerService
                .startContainer(novncContainer);
        containers.put(containerId, novncImage);

        String novncHost = dockerService.getDockerGateway(containerId, network);
        String novncPort = dockerService.getBindPort(containerId,
                defaultNovncPort + "/tcp");
        return format(
                "http://%s:%s/vnc.html?host=%s&port=%d&path=vnc/%s&resize=scale&autoconnect=true&password=%s",
                novncHost, novncPort, selenoidHost, selenoidPort, sessionId,
                novncPassword);
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

    private void waitForRecording() throws IOException {
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

        log.trace("Renaming {} to {}.mp4", recordingFile, name);
        move(recordingFile.toPath(),
                recordingFile.toPath().resolveSibling(name + ".mp4"),
                REPLACE_EXISTING);
    }

}
