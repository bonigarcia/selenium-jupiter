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

import java.io.File;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Selenium extension for Jupiter (JUnit 5) tests.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumExtension implements ParameterResolver, AfterEachCallback {

    public static final String ARGS = "args";
    public static final String BINARY = "binary";
    public static final String EXTENSIONS = "extensions";
    public static final String EXTENSION_FILES = "extensionFiles";
    public static final String PAGE_LOAD_STRATEGY = "pageLoadStrategy";

    protected static final Logger log = LoggerFactory
            .getLogger(SeleniumExtension.class);

    private List<WebDriver> webDriverList = new ArrayList<>();
    private List<Class<?>> typeList = new ArrayList<>();

    @Override
    public boolean supports(ParameterContext parameterContext,
            ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return WebDriver.class.isAssignableFrom(type) && !type.isInterface();
    }

    @Override
    public Object resolve(ParameterContext parameterContext,
            ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Parameter parameter = parameterContext.getParameter();

        Class<?> type = parameter.getType();

        // WebDriverManager
        if (!typeList.contains(type)) {
            typeList.add(type);
            WebDriverManager.getInstance(type).setup();
        }

        WebDriver webDriver = null;
        Capabilities capabilities = getCapabilities(parameter);

        if (type == ChromeDriver.class) {
            ChromeOptions chromeOptions = getChromeOptions(parameter);
            ((DesiredCapabilities) capabilities)
                    .setCapability(ChromeOptions.CAPABILITY, chromeOptions);
            webDriver = new ChromeDriver(capabilities);

        } else if (type == FirefoxDriver.class) {
            FirefoxOptions firefoxOptions = getFirefoxOptions(parameter);
            firefoxOptions.addCapabilities(capabilities);
            webDriver = new FirefoxDriver(firefoxOptions);

        } else if (type == EdgeDriver.class) {
            EdgeOptions edgeOptions = getEdgeOptions(parameter);
            ((DesiredCapabilities) capabilities)
                    .setCapability(EdgeOptions.CAPABILITY, edgeOptions);
            webDriver = new EdgeDriver(capabilities);

        } else if (type == RemoteWebDriver.class) {
            Optional<URL> url = getUrl(parameter);
            if (url.isPresent()) {
                webDriver = new RemoteWebDriver(url.get(), capabilities);
            } else {
                log.warn(
                        "Was not possible to instantiate RemoteWebDriver, URL not present");
            }

        } else {
            // Any other
            try {
                webDriver = (WebDriver) type.newInstance();
            } catch (Exception e) {
                throw new ParameterResolutionException(
                        "Exception creating instance of " + type.getName(), e);
            }
        }

        if (webDriver != null) {
            webDriverList.add(webDriver);
        }

        return webDriver;
    }

    @Override
    public void afterEach(TestExtensionContext context) throws Exception {
        webDriverList.forEach(webdriver -> webdriver.quit());
        webDriverList.clear();
    }

    private ChromeOptions getChromeOptions(Parameter parameter) {
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);
        ChromeOptions chromeOptions = new ChromeOptions();
        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();
                switch (name) {
                case ARGS:
                    chromeOptions.addArguments(value);
                    break;
                case BINARY:
                    chromeOptions.setBinary(value);
                    break;
                case EXTENSIONS:
                    chromeOptions.addEncodedExtensions(value);
                    break;
                case EXTENSION_FILES:
                    chromeOptions.addExtensions(new File(value));
                    break;
                default:
                    chromeOptions.setExperimentalOption(name, value);
                }
            }
        }
        return chromeOptions;
    }

    private FirefoxOptions getFirefoxOptions(Parameter parameter) {
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();
                switch (name) {
                case ARGS:
                    firefoxOptions.addArguments(value);
                    break;
                case BINARY:
                    firefoxOptions.setBinary(value);
                    break;
                default:
                    if (isBoolean(value)) {
                        firefoxOptions.addPreference(name,
                                Boolean.valueOf(value));
                    } else if (isNumeric(value)) {
                        firefoxOptions.addPreference(name,
                                Integer.parseInt(value));
                    } else {
                        firefoxOptions.addPreference(name, value);
                    }
                }
            }
        }
        return firefoxOptions;
    }

    private EdgeOptions getEdgeOptions(Parameter parameter) {
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);
        EdgeOptions edgeOptions = new EdgeOptions();
        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();
                switch (name) {
                case PAGE_LOAD_STRATEGY:
                    edgeOptions.setPageLoadStrategy(value);
                    break;
                default:
                    log.warn("Option {} not supported for Edge", name);
                }
            }
        }
        return edgeOptions;
    }

    private Capabilities getCapabilities(Parameter parameter) {
        DriverCapabilities driverCapabilities = parameter
                .getAnnotation(DriverCapabilities.class);
        Capabilities capabilities = new DesiredCapabilities();

        if (driverCapabilities != null) {
            for (Capability capability : driverCapabilities.capability()) {
                ((DesiredCapabilities) capabilities)
                        .setCapability(capability.name(), capability.value());
            }
        }
        return capabilities;
    }

    private Optional<URL> getUrl(Parameter parameter) {
        DriverUrl driverUrl = parameter.getAnnotation(DriverUrl.class);
        Optional<URL> out = Optional.empty();
        if (driverUrl != null) {
            String value = driverUrl.value();
            try {
                out = Optional.of(new URL(value));
            } catch (MalformedURLException e) {
                log.warn("Bad URL {}", value, e);
            }
        }
        return out;
    }

    private boolean isBoolean(String s) {
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false");
    }

    private boolean isNumeric(String s) {
        return StringUtils.isNumeric(s);
    }

}
