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
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    static DockerDriverHandler instance;
    DockerService dockerService;
    List<String> containers;
    Path tmpDir;

    public static synchronized DockerDriverHandler getInstance() {
        if (instance == null) {
            instance = new DockerDriverHandler();
        }
        return instance;
    }

    public WebDriver resolve(BrowserType browser, Parameter parameter,
            Optional<Object> testInstance) {
        WebDriver webDriver = null;

        if (dockerService == null) {
            dockerService = new DockerService();
        }
        if (containers == null) {
            containers = new ArrayList<>();
        }

        try {
            Optional<String> version = AnnotationsReader.getInstance()
                    .getVersion(parameter);

            boolean enableVnc = getBoolean("sel.jup.docker.vnc");
            boolean recording = getBoolean("sel.jup.docker.recording");
            int selenoidPort = startDockerBrowser(browser, version, recording);
            int novncPort = 0;
            if (enableVnc) {
                novncPort = startDockerNoVnc();
            }

            Class<? extends RemoteWebDriver> driverClass = browser
                    .getDriverClass();

            DesiredCapabilities capabilities = browser.getCapabilities();

            if (version.isPresent()) {
                capabilities.setCapability("version", SelenoidConfig
                        .getInstance().getImageVersion(browser, version.get()));
            }

            if (enableVnc) {
                capabilities.setCapability("enableVNC", true);
            }

            if (recording) {
                capabilities.setCapability("enableVideo", true);
            }

            Optional<Capabilities> optionalCapabilities = AnnotationsReader
                    .getInstance().getCapabilities(parameter, testInstance);
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
            webDriver = driverClass
                    .getConstructor(URL.class, Capabilities.class)
                    .newInstance(new URL(selenoidHubUrl), capabilities);
            if (enableVnc) {
                SessionId sessionId = ((RemoteWebDriver) webDriver)
                        .getSessionId();
                String vncUrl = format(
                        "http://%s:%d/vnc.html?host=%s&port=%d&path=vnc/%s&resize=scale&autoconnect=true&password=%s",
                        dockerServerIp, novncPort, dockerServerIp, selenoidPort,
                        sessionId, getString("sel.jup.selenoid.vnc.password"));
                log.debug("Session {} VNC URL: {}", sessionId, vncUrl);
            }

            return webDriver;

        } catch (Exception e) {
            throw new SeleniumJupiterException(e);
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
        containers.add(novncContainerName);

        return novncPort;
    }

    private int startDockerBrowser(BrowserType browser,
            Optional<String> version, boolean recording) throws IOException {
        String selenoidImage = getString("sel.jup.selenoid.image");
        String recordingImage = getString("sel.jup.docker.recording.image");
        String browserImage = version.isPresent() && !version.get().isEmpty()
                ? SelenoidConfig.getInstance().getImageFromVersion(browser,
                        version.get())
                : SelenoidConfig.getInstance().getLatestImage(browser);

        dockerService.pullImageIfNecessary(selenoidImage);
        dockerService.pullImageIfNecessary(browserImage);
        if (recording) {
            dockerService.pullImageIfNecessary(recordingImage);
        }

        String defaultSocket = dockerService.getDockerDefaultSocket();
        Volume defaultSocketVolume = new Volume(defaultSocket);
        Volume selenoidConfigVolume = new Volume("/etc/selenoid");

        List<Volume> volumes = new ArrayList<>();
        volumes.add(defaultSocketVolume);
        volumes.add(selenoidConfigVolume);
        String video = "/opt/selenoid/video";
        String hostVideo = new File(
                getString("sel.jup.docker.recording.folder")).getAbsolutePath();
        Volume videoVolume = new Volume(video);
        if (recording) {
            volumes.add(videoVolume);
        }

        tmpDir = createTempDirectory("");
        String browsersJson = SelenoidConfig.getInstance()
                .getBrowsersJsonAsString();
        writeStringToFile(new File(tmpDir.toFile(), "browsers.json"),
                browsersJson, defaultCharset());
        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(defaultSocket, defaultSocketVolume));
        binds.add(new Bind(tmpDir.toFile().toString(), selenoidConfigVolume));
        if (recording) {
            binds.add(new Bind(hostVideo, videoVolume));
        }

        int selenoidPort = dockerService.findRandomOpenPort();
        Binding selenoidBindPort = bindPort(selenoidPort);
        ExposedPort selenoidExposedPort = tcp(getInt("sel.jup.selenoid.port"));
        List<PortBinding> portBindings = asList(
                new PortBinding(selenoidBindPort, selenoidExposedPort));
        String selenoidContainerName = dockerService
                .generateContainerName("selenoid");
        DockerBuilder dockerBuilder = DockerContainer
                .dockerBuilder(selenoidImage, selenoidContainerName)
                .portBindings(portBindings).volumes(volumes).binds(binds);
        if (recording) {
            List<String> envs = asList(
                    "OVERRIDE_VIDEO_OUTPUT_DIR=" + hostVideo);
            dockerBuilder.envs(envs);
        }

        DockerContainer selenoidContainer = dockerBuilder.build();
        dockerService.startAndWaitContainer(selenoidContainer);
        containers.add(selenoidContainerName);

        return selenoidPort;
    }

    public void clearContainersIfNecessary() {
        if (containers != null && dockerService != null) {
            containers.forEach(dockerService::stopAndRemoveContainer);
            containers.clear();
        }
        if (tmpDir != null) {
            try {
                deleteDirectory(tmpDir.toFile());
            } catch (IOException e) {
                log.warn("Exception deleting temporal folder {}", tmpDir, e);
            }
        }
    }

}
