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

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static java.io.File.separator;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import java.io.File;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Selenium extension for Jupiter (JUnit 5) tests.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumExtension implements ParameterResolver, AfterEachCallback {

    protected static final Logger log = LoggerFactory
            .getLogger(SeleniumExtension.class);

    private List<WebDriver> webDriverList = new ArrayList<>();
    private List<Class<?>> typeList = new ArrayList<>();
    private AnnotationsReader annotationsReader = new AnnotationsReader();
    private AppiumDriverLocalService appiumDriverLocalService;
    private DockerService dockerService;
    private List<String> containers;

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return WebDriver.class.isAssignableFrom(type) && !type.isInterface();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        Parameter parameter = parameterContext.getParameter();
        Optional<Object> testInstance = extensionContext.getTestInstance();
        Class<?> type = parameter.getType();

        // WebDriverManager
        if (!typeList.contains(type)) {
            typeList.add(type);
            WebDriverManager.getInstance(type).setup();
        }

        // Instantiate WebDriver
        WebDriver webDriver = null;
        Optional<Capabilities> capabilities = annotationsReader
                .getCapabilities(parameter, testInstance);

        if (type == ChromeDriver.class) {
            ChromeOptions chromeOptions = annotationsReader
                    .getChromeOptions(parameter, testInstance);
            if (capabilities.isPresent()) {
                ((DesiredCapabilities) capabilities.get())
                        .setCapability(ChromeOptions.CAPABILITY, chromeOptions);
                webDriver = new ChromeDriver(capabilities.get());
            } else {
                webDriver = new ChromeDriver(chromeOptions);
            }

        } else if (type == FirefoxDriver.class) {
            FirefoxOptions firefoxOptions = annotationsReader
                    .getFirefoxOptions(parameter, testInstance);
            if (capabilities.isPresent()) {
                firefoxOptions.addCapabilities(capabilities.get());
            }
            webDriver = new FirefoxDriver(firefoxOptions);

        } else if (type == EdgeDriver.class) {
            EdgeOptions edgeOptions = annotationsReader
                    .getEdgeOptions(parameter, testInstance);
            if (capabilities.isPresent()) {
                ((DesiredCapabilities) capabilities.get())
                        .setCapability(EdgeOptions.CAPABILITY, edgeOptions);
                webDriver = new EdgeDriver(capabilities.get());
            } else {
                webDriver = new EdgeDriver(edgeOptions);
            }

        } else if (type == OperaDriver.class) {
            OperaOptions operaOptions = annotationsReader
                    .getOperaOptions(parameter, testInstance);
            if (capabilities.isPresent()) {
                ((DesiredCapabilities) capabilities.get())
                        .setCapability(OperaOptions.CAPABILITY, operaOptions);
                webDriver = new OperaDriver(capabilities.get());
            } else {
                webDriver = new OperaDriver(operaOptions);
            }

        } else if (type == SafariDriver.class) {
            SafariOptions safariOptions = annotationsReader
                    .getSafariOptions(parameter, testInstance);
            if (capabilities.isPresent()) {
                ((DesiredCapabilities) capabilities.get())
                        .setCapability(SafariOptions.CAPABILITY, safariOptions);
                webDriver = new SafariDriver(capabilities.get());
            } else {
                webDriver = new SafariDriver(safariOptions);
            }

        } else if (type == RemoteWebDriver.class) {
            Optional<URL> url = annotationsReader.getUrl(parameter,
                    testInstance);
            if (url.isPresent() && capabilities.isPresent()) {
                webDriver = new RemoteWebDriver(url.get(), capabilities.get());
            } else {
                String urlMessage = url.isPresent() ? "" : "URL not present ";
                String capabilitiesMessage = capabilities.isPresent() ? ""
                        : "Capabilites not present";
                log.warn(
                        "Was not possible to instantiate RemoteWebDriver: {}{}",
                        urlMessage, capabilitiesMessage);
            }

        } else if (type == AppiumDriver.class) {
            Optional<URL> url = annotationsReader.getUrl(parameter,
                    testInstance);
            if (capabilities.isPresent()) {
                URL appiumServerUrl;
                if (url.isPresent()) {
                    appiumServerUrl = url.get();
                } else {
                    appiumDriverLocalService = AppiumDriverLocalService
                            .buildDefaultService();
                    appiumDriverLocalService.start();
                    appiumServerUrl = appiumDriverLocalService.getUrl();
                }

                webDriver = new AndroidDriver<>(appiumServerUrl,
                        capabilities.get());
            } else {
                log.warn(
                        "Was not possible to instantiate AppiumDriver: Capabilites not present");
            }

        } else if (type == DockerChromeDriver.class) {

            dockerService = new DockerService();
            containers = new ArrayList<>();
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
                            if (jarEntry.getName()
                                    .startsWith(browserJsonName)) {
                                jar.getInputStream(jarEntry);
                                Path tmpDir = createTempDirectory(
                                        "browsersJson");
                                parent = tmpDir.toFile().toString();
                                File destination = new File(
                                        parent + separator + browserJsonName);
                                copyInputStreamToFile(
                                        jar.getInputStream(jarEntry),
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
                        .portBindings(portBindings).volumes(volumes)
                        .binds(binds).build();
                dockerService.startAndWaitContainer(dockerContainer);
                containers.add(selenoidContainerName);

                webDriver = new DockerChromeDriver(
                        new URL("http://" + dockerService.getDockerServerIp()
                                + ":" + freePort + "/wd/hub"),
                        DesiredCapabilities.chrome());
            } catch (Exception e) {
                String errorMessage = "Exception creating instance of DockerChromeDriver";
                log.error(errorMessage, e);
                throw new SeleniumJupiterException(errorMessage, e);
            }

        } else {
            // Other WebDriver type
            try {
                if (capabilities.isPresent()) {
                    webDriver = (WebDriver) type
                            .getDeclaredConstructor(Capabilities.class)
                            .newInstance(capabilities.get());
                } else {
                    webDriver = (WebDriver) type.newInstance();
                }

            } catch (Exception e) {
                String errorMessage = "Exception creating instance of "
                        + type.getName();
                log.error(errorMessage, e);
                throw new SeleniumJupiterException(errorMessage, e);
            }
        }

        if (webDriver != null) {
            webDriverList.add(webDriver);
        }

        return webDriver;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        webDriverList.forEach(WebDriver::quit);
        webDriverList.clear();

        if (appiumDriverLocalService != null) {
            appiumDriverLocalService.stop();
        }

        if (dockerService != null) {
            containers.forEach(dockerService::stopAndRemoveContainer);
        }
    }
}
