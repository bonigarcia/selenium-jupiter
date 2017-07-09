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

import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
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
    private Options optionsParser = new Options();

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return WebDriver.class.isAssignableFrom(type) && !type.isInterface();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext)
            throws ParameterResolutionException {
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
        Capabilities capabilities = optionsParser.getCapabilities(parameter);

        if (type == ChromeDriver.class) {
            ChromeOptions chromeOptions = optionsParser
                    .getChromeOptions(parameter);
            ((DesiredCapabilities) capabilities)
                    .setCapability(ChromeOptions.CAPABILITY, chromeOptions);
            webDriver = new ChromeDriver(capabilities);

        } else if (type == FirefoxDriver.class) {
            FirefoxOptions firefoxOptions = optionsParser
                    .getFirefoxOptions(parameter);
            firefoxOptions.addCapabilities(capabilities);
            webDriver = new FirefoxDriver(firefoxOptions);

        } else if (type == EdgeDriver.class) {
            EdgeOptions edgeOptions = optionsParser.getEdgeOptions(parameter);
            ((DesiredCapabilities) capabilities)
                    .setCapability(EdgeOptions.CAPABILITY, edgeOptions);
            webDriver = new EdgeDriver(capabilities);

        } else if (type == OperaDriver.class) {
            OperaOptions operaOptions = optionsParser
                    .getOperaOptions(parameter);
            ((DesiredCapabilities) capabilities)
                    .setCapability(OperaOptions.CAPABILITY, operaOptions);
            webDriver = new OperaDriver(capabilities);

        } else if (type == SafariDriver.class) {
            SafariOptions safariOptions = optionsParser
                    .getSafariOptions(parameter);
            ((DesiredCapabilities) capabilities)
                    .setCapability(SafariOptions.CAPABILITY, safariOptions);
            webDriver = new SafariDriver(capabilities);

        } else if (type == RemoteWebDriver.class) {
            Optional<URL> url = optionsParser.getUrl(parameter, testInstance);
            if (url.isPresent()) {
                webDriver = new RemoteWebDriver(url.get(), capabilities);
            } else {
                log.warn("Was not possible to instantiate RemoteWebDriver,"
                        + " URL not present");
            }

        } else {
            // Other WebDriver type
            try {
                webDriver = (WebDriver) type
                        .getDeclaredConstructor(Capabilities.class)
                        .newInstance(capabilities);
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
    public void afterEach(ExtensionContext context) throws Exception {
        webDriverList.forEach(webdriver -> webdriver.quit());
        webDriverList.clear();
    }
}
