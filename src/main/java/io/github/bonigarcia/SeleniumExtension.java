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

import static java.lang.invoke.MethodHandles.lookup;
import static org.openqa.selenium.OutputType.BASE64;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;

import io.appium.java_client.AppiumDriver;
import io.github.bonigarcia.handler.AppiumDriverHandler;
import io.github.bonigarcia.handler.ChromeDriverHandler;
import io.github.bonigarcia.handler.DriverHandler;
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

    final Logger log = getLogger(lookup().lookupClass());

    private List<WebDriver> webDriverList = new ArrayList<>();
    private List<Class<?>> typeList = new ArrayList<>();
    private DriverHandler driverHandler;

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

        // Handler
        if (type == ChromeDriver.class) {
            driverHandler = new ChromeDriverHandler(parameter, testInstance);

        } else if (type == FirefoxDriver.class) {
            driverHandler = new FirefoxDriverHandler(parameter, testInstance);

        } else if (type == EdgeDriver.class) {
            driverHandler = new EdgeDriverHandler(parameter, testInstance);

        } else if (type == OperaDriver.class) {
            driverHandler = new OperaDriverHandler(parameter, testInstance);

        } else if (type == SafariDriver.class) {
            driverHandler = new SafariDriverHandler(parameter, testInstance);

        } else if (type == RemoteWebDriver.class) {
            driverHandler = new RemoteDriverHandler(parameter, testInstance);

        } else if (type == AppiumDriver.class) {
            driverHandler = new AppiumDriverHandler(parameter, testInstance);

        } else {
            driverHandler = new OtherDriverHandler(parameter, testInstance);
        }

        WebDriver webDriver = driverHandler.resolve();
        if (webDriver != null) {
            webDriverList.add(webDriver);
        }

        return webDriver;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Optional<Throwable> executionException = context
                .getExecutionException();
        if (executionException.isPresent()) {
            webDriverList.forEach(this::logBase64Screenshot);
        }

        webDriverList.forEach(WebDriver::quit);
        webDriverList.clear();

        driverHandler.cleanup();
    }

    void logBase64Screenshot(WebDriver driver) {
        if (driver != null) {
            try {
                String screenshotBase64 = ((TakesScreenshot) driver)
                        .getScreenshotAs(BASE64);
                log.debug("Screenshot (in Base64) at the end of session {} "
                        + "(copy&paste this string as URL in browser to watch it):\r\n"
                        + "data:image/png;base64,{}",
                        ((RemoteWebDriver) driver).getSessionId(),
                        screenshotBase64);
            } catch (Exception e) {
                log.trace("Exception getting screenshot", e);
            }
        }
    }
}
