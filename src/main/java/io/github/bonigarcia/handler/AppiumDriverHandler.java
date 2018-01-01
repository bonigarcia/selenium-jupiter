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

import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.github.bonigarcia.SeleniumJupiterException;

/**
 * Resolver for AppiumDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class AppiumDriverHandler extends DriverHandler {

    private AppiumDriverLocalService appiumDriverLocalService;

    public AppiumDriverHandler(Parameter parameter,
            Optional<Object> testInstance) {
        super(parameter, testInstance);
    }

    @Override
    public WebDriver resolve() {
        WebDriver webDriver = null;
        try {
            Optional<Capabilities> capabilities = annotationsReader
                    .getCapabilities(parameter, testInstance);

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
                String noCapsMessage = "Was not possible to instantiate AppiumDriver: Capabilites not present";
                if (throwExceptionWhenNoDriver()) {
                    throw new SeleniumJupiterException(noCapsMessage);
                } else {
                    log.warn(noCapsMessage);
                }
            }
        } catch (Exception e) {
            handleException(e);
        }
        return webDriver;
    }

    @Override
    public void cleanup() {
        if (appiumDriverLocalService != null) {
            appiumDriverLocalService.stop();
            appiumDriverLocalService = null;
        }
    }

}
