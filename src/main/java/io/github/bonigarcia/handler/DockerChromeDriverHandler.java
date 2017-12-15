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
import static java.io.File.separator;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;

import io.github.bonigarcia.DockerChromeDriver;
import io.github.bonigarcia.DockerContainer;
import io.github.bonigarcia.DockerService;
import io.github.bonigarcia.SeleniumExtension;
import io.github.bonigarcia.SeleniumJupiterException;

/**
 * Resolver for DockerChromeDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class DockerChromeDriverHandler {

    final Logger log = getLogger(lookup().lookupClass());

    static DockerChromeDriverHandler instance;
    DockerService dockerService;
    List<String> containers;

    public static synchronized DockerChromeDriverHandler getInstance() {
        if (instance == null) {
            instance = new DockerChromeDriverHandler();
        }
        return instance;
    }

    public WebDriver resolve() {
        WebDriver webDriver = null;

        if (dockerService == null) {
            dockerService = new DockerService();
        }
        if (containers == null) {
            containers = new ArrayList<>();
        }

        String selenoidImage = "aerokube/selenoid:1.3.7";
        dockerService.pullImageIfNecessary(selenoidImage);
        dockerService.pullImageIfNecessary("selenoid/vnc:chrome_61.0");

        String dockerDefaultSocket = dockerService.getDockerDefaultSocket();
        Volume volume = new Volume(dockerDefaultSocket);
        Volume resources = new Volume("/etc/selenoid");
        List<Volume> volumes = asList(volume, resources);

        try {
            String parent = null;
            String browserJsonName = "browsers.json";
            File jarFile = new File(getClass().getProtectionDomain()
                    .getCodeSource().getLocation().getPath());

            if (jarFile.isFile()) {
                try (JarFile jar = new JarFile(jarFile)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement();
                        if (jarEntry.getName().startsWith(browserJsonName)) {
                            jar.getInputStream(jarEntry);
                            Path tmpDir = createTempDirectory("browsersJson");
                            parent = tmpDir.toFile().toString();
                            File destination = new File(
                                    parent + separator + browserJsonName);
                            copyInputStreamToFile(jar.getInputStream(jarEntry),
                                    destination);
                            break;
                        }
                    }
                }
            } else { // Development
                URL browsersJsonUrl = SeleniumExtension.class
                        .getResource("/" + browserJsonName);
                parent = Paths.get(browsersJsonUrl.toURI()).toFile()
                        .getParent();
            }

            List<Bind> binds = asList(new Bind(dockerDefaultSocket, volume),
                    new Bind(parent, resources));
            int freePort = dockerService.findRandomOpenPort();
            Binding bindPort = bindPort(freePort);
            ExposedPort exposedPort = tcp(4444);

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

            webDriver = new DockerChromeDriver(
                    new URL("http://" + dockerService.getDockerServerIp() + ":"
                            + freePort + "/wd/hub"),
                    DesiredCapabilities.chrome());
            return webDriver;

        } catch (Exception e) {
            String errorMessage = "Exception creating instance of DockerChromeDriver";
            log.error(errorMessage, e);
            throw new SeleniumJupiterException(errorMessage, e);
        }
    }

    public void clearContainersIfNecessary() {
        if (containers != null && dockerService != null) {
            containers.forEach(dockerService::stopAndRemoveContainer);
            containers.clear();
        }
    }

}
