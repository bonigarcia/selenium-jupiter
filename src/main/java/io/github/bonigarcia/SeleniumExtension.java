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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.appium.java_client.AppiumDriver;
import io.github.bonigarcia.handler.AppiumDriverHandler;
import io.github.bonigarcia.handler.ChromeDriverHandler;
import io.github.bonigarcia.handler.DockerChromeDriverHandler;
import io.github.bonigarcia.handler.EdgeDriverHandler;
import io.github.bonigarcia.handler.FirefoxDriverHandler;
import io.github.bonigarcia.handler.OperaDriverHandler;
import io.github.bonigarcia.handler.OtherDriverHandler;
import io.github.bonigarcia.handler.RemoteDriverHandler;
import io.github.bonigarcia.handler.SafariDriverHandler;
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

        if (type == ChromeDriver.class) {
            webDriver = ChromeDriverHandler.getInstance().resolve(parameter,
                    testInstance);

        } else if (type == FirefoxDriver.class) {
            webDriver = FirefoxDriverHandler.getInstance().resolve(parameter,
                    testInstance);

        } else if (type == EdgeDriver.class) {
            webDriver = EdgeDriverHandler.getInstance().resolve(parameter,
                    testInstance);

        } else if (type == OperaDriver.class) {
            webDriver = OperaDriverHandler.getInstance().resolve(parameter,
                    testInstance);

        } else if (type == SafariDriver.class) {
            webDriver = SafariDriverHandler.getInstance().resolve(parameter,
                    testInstance);

        } else if (type == RemoteWebDriver.class) {
            webDriver = RemoteDriverHandler.getInstance().resolve(parameter,
                    testInstance);

        } else if (type == AppiumDriver.class) {
            webDriver = AppiumDriverHandler.getInstance().resolve(parameter,
                    testInstance);

        } else if (type == DockerChromeDriver.class) {
            webDriver = DockerChromeDriverHandler.getInstance()
                    .resolve(parameter, testInstance);

        } else {
            // Other WebDriver type
            webDriver = OtherDriverHandler.getInstance().resolve(parameter,
                    testInstance);
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

        AppiumDriverHandler.getInstance().closeLocalServiceIfNecessary();
        DockerChromeDriverHandler.getInstance().clearContainersIfNecessary();
    }
}
