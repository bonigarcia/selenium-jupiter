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

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.github.bonigarcia.AnnotationsReader;

/**
 * Resolver for AppiumDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class AppiumDriverHandler {

    final Logger log = getLogger(lookup().lookupClass());

    static AppiumDriverHandler instance;
    private AppiumDriverLocalService appiumDriverLocalService;

    public static synchronized AppiumDriverHandler getInstance() {
        if (instance == null) {
            instance = new AppiumDriverHandler();
        }
        return instance;
    }

    public WebDriver resolve(Parameter parameter,
            Optional<Object> testInstance) {
        WebDriver webDriver = null;
        Optional<Capabilities> capabilities = AnnotationsReader.getInstance()
                .getCapabilities(parameter, testInstance);

        Optional<URL> url = AnnotationsReader.getInstance().getUrl(parameter,
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

        return webDriver;
    }

    public void closeLocalServiceIfNecessary() {
        if (appiumDriverLocalService != null) {
            appiumDriverLocalService.stop();
            appiumDriverLocalService = null;
        }
    }

}
