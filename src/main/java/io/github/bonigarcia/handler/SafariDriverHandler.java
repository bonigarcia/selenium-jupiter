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
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import io.github.bonigarcia.Options;

/**
 * Resolver for SafariDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class SafariDriverHandler extends DriverHandler {

    public SafariDriverHandler(Parameter parameter, ExtensionContext context) {
        super(parameter, context);
    }

    @Override
    public WebDriver resolve() {
        SafariDriver driver = null;
        try {
            Optional<Object> testInstance = context.getTestInstance();
            Optional<Capabilities> capabilities = annotationsReader
                    .getCapabilities(parameter, testInstance);
            SafariOptions safariOptions = (SafariOptions) getOptions(parameter,
                    testInstance);
            if (capabilities.isPresent()) {
                safariOptions.merge(capabilities.get());
            }
            driver = new SafariDriver(safariOptions);
        } catch (Exception e) {
            handleException(e);
        }
        return driver;
    }

    @Override
    public MutableCapabilities getOptions(Parameter parameter,
            Optional<Object> testInstance) throws IllegalAccessException {
        SafariOptions safariOptions = new SafariOptions();
        Object optionsFromAnnotatedField = annotationsReader
                .getOptionsFromAnnotatedField(testInstance, Options.class);
        if (optionsFromAnnotatedField != null) {
            safariOptions = (SafariOptions) optionsFromAnnotatedField;
        }
        return safariOptions;
    }

}
