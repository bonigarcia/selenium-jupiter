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
package io.github.bonigarcia.seljup.handler;

import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.SeleniumJupiterException;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Resolver for AppiumDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class AppiumDriverHandler extends DriverHandler {

    private AppiumDriverLocalService appiumDriverLocalService;

    public AppiumDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader) {
        super(parameter, context, config, annotationsReader);
    }

    @Override
    public void resolve() {
        try {
            Optional<Object> testInstance = context.getTestInstance();
            Optional<Capabilities> capabilities = annotationsReader
                    .getCapabilities(parameter, testInstance);

            Optional<URL> url = annotationsReader.getUrl(parameter,
                    testInstance, config.getSeleniumServerUrl());
            if (capabilities.isPresent()) {
                URL appiumServerUrl;
                if (url.isPresent()) {
                    appiumServerUrl = url.get();
                } else {
                    AppiumServiceBuilder builder = new AppiumServiceBuilder();
                    builder.withArgument(GeneralServerFlag.LOG_LEVEL, config.getAndroidAppiumLogLevel());
                    appiumDriverLocalService = AppiumDriverLocalService.buildService(builder);
                    appiumDriverLocalService.start();
                    appiumServerUrl = appiumDriverLocalService.getUrl();
                }

                object = new AndroidDriver<>(appiumServerUrl,
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
    }

    @Override
    public void cleanup() {
        if (appiumDriverLocalService != null) {
            appiumDriverLocalService.stop();
            appiumDriverLocalService = null;
        }
    }

}
