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
import static io.github.bonigarcia.SeleniumJupiter.getString;
import static io.github.bonigarcia.SelenoidBrowser.getDockerContainerPort;
import static java.io.File.separator;
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;

import io.github.bonigarcia.AnnotationsReader;
import io.github.bonigarcia.DockerContainer;
import io.github.bonigarcia.DockerService;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.SelenoidBrowser;
import io.github.bonigarcia.SelenoidConfig;

/**
 * Resolver for DockerDriver's.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class DockerDriverHandler {

    final Logger log = getLogger(lookup().lookupClass());

    final static String BROWSER_JSON_FILENAME = "browsers.json";

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

    public WebDriver resolve(SelenoidBrowser browser, Parameter parameter,
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
                    .getVersion(parameter, testInstance);

            SelenoidConfig selenoidConfig = new SelenoidConfig();

            String selenoidImage = getString("sel.jup.selenoid.image");
            String browserImage = version.isPresent()
                    && !version.get().isEmpty()
                            ? selenoidConfig.getImageFromVersion(browser,
                                    version.get())
                            : selenoidConfig.getLatestImage(browser);

            dockerService.pullImageIfNecessary(selenoidImage);
            dockerService.pullImageIfNecessary(browserImage);

            String dockerDefaultSocket = dockerService.getDockerDefaultSocket();
            Volume volume = new Volume(dockerDefaultSocket);
            Volume resources = new Volume("/etc/selenoid");
            List<Volume> volumes = asList(volume, resources);

            tmpDir = createTempDirectory(BROWSER_JSON_FILENAME);

            String browsersJson = selenoidConfig.getBrowsersJsonAsString();
            writeStringToFile(
                    new File(tmpDir + separator + BROWSER_JSON_FILENAME),
                    browsersJson, defaultCharset());
            List<Bind> binds = asList(new Bind(dockerDefaultSocket, volume),
                    new Bind(tmpDir.toFile().toString(), resources));

            int freePort = dockerService.findRandomOpenPort();
            Binding bindPort = bindPort(freePort);
            ExposedPort exposedPort = tcp(getDockerContainerPort());

            List<PortBinding> portBindings = asList(
                    new PortBinding(bindPort, exposedPort));
            String selenoidContainerName = dockerService
                    .generateContainerName("selenoid");
            DockerContainer dockerContainer = DockerContainer
                    .dockerBuilder(selenoidImage, selenoidContainerName)
                    .portBindings(portBindings).volumes(volumes).binds(binds)
                    .build();
            dockerService.startAndWaitContainer(dockerContainer);
            containers.add(selenoidContainerName);

            Class<? extends RemoteWebDriver> driverClass = browser
                    .getDriverClass();
            DesiredCapabilities capabilities = browser.getCapabilities();

            if (version.isPresent()) {
                capabilities.setCapability("version",
                        selenoidConfig.getImageVersion(browser, version.get()));
            }

            webDriver = driverClass
                    .getConstructor(URL.class, Capabilities.class)
                    .newInstance(new URL(
                            "http://" + dockerService.getDockerServerIp() + ":"
                                    + freePort + "/wd/hub"),
                            capabilities);

            return webDriver;

        } catch (Exception e) {
            throw new SeleniumJupiterException(e);
        }

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
