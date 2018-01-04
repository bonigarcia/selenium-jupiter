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
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import io.appium.java_client.AppiumDriver;
import io.github.bonigarcia.handler.AppiumDriverHandler;
import io.github.bonigarcia.handler.ChromeDriverHandler;
import io.github.bonigarcia.handler.DriverHandler;
import io.github.bonigarcia.handler.EdgeDriverHandler;
import io.github.bonigarcia.handler.FirefoxDriverHandler;
import io.github.bonigarcia.handler.ListDriverHandler;
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
    private Map<Class<?>, Class<? extends DriverHandler>> handlerMap = new HashMap<>();
    {
        handlerMap.put(ChromeDriver.class, ChromeDriverHandler.class);
        handlerMap.put(FirefoxDriver.class, FirefoxDriverHandler.class);
        handlerMap.put(EdgeDriver.class, EdgeDriverHandler.class);
        handlerMap.put(OperaDriver.class, OperaDriverHandler.class);
        handlerMap.put(SafariDriver.class, SafariDriverHandler.class);
        handlerMap.put(RemoteWebDriver.class, RemoteDriverHandler.class);
        handlerMap.put(AppiumDriver.class, AppiumDriverHandler.class);
        handlerMap.put(List.class, ListDriverHandler.class);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return (WebDriver.class.isAssignableFrom(type) && !type.isInterface())
                || List.class.isAssignableFrom(type);
    }

    @SuppressWarnings("unchecked")
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
        Class<? extends DriverHandler> constructorClass = handlerMap
                .containsKey(type) ? handlerMap.get(type)
                        : OtherDriverHandler.class;
        try {
            driverHandler = constructorClass
                    .getDeclaredConstructor(Parameter.class, Optional.class)
                    .newInstance(parameter, testInstance);
        } catch (Exception e) {
            log.warn("Exception creating {}", constructorClass);
        }

        Object out = driverHandler.resolve();
        if (type == List.class) {
            ((List<RemoteWebDriver>) out).forEach(this::addWebDriverToList);
        } else {
            addWebDriverToList((WebDriver) out);
        }

        return out;
    }

    private void addWebDriverToList(WebDriver webDriver) {
        if (webDriver != null) {
            webDriverList.add((WebDriver) webDriver);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ScreenshotManager screenshotManager = new ScreenshotManager(context);

        if (screenshotManager.isScreenshotRequired()) {
            webDriverList.forEach(screenshotManager::makeScreenshot);
        }

        for (WebDriver webDriver : webDriverList) {
            try {
                webDriver.quit();
            } catch (Exception e) {
                log.warn("Exception closing webdriver instance {}", webDriver,
                        e);
            }
        }
        webDriverList.clear();

        if (driverHandler != null) {
            try {
                driverHandler.cleanup();
            } catch (Exception e) {
                log.warn("Exception cleaning handler {}", driverHandler, e);
            }
        }
    }

}
