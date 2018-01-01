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

import static io.github.bonigarcia.SeleniumJupiter.USE_CLEAN_SESSION;
import static io.github.bonigarcia.SeleniumJupiter.USE_TECHNOLOGY_PREVIEW;
import static java.lang.Boolean.valueOf;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import io.github.bonigarcia.DriverOptions;
import io.github.bonigarcia.Option;

/**
 * Resolver for SafariDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class SafariDriverHandler extends DriverHandler {

    public SafariDriverHandler(Parameter parameter,
            Optional<Object> testInstance) {
        super(parameter, testInstance);
    }

    @Override
    public WebDriver resolve() {
        SafariDriver driver = null;
        try {
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
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);

        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();
                switch (name) {
                case USE_CLEAN_SESSION:
                    assert annotationsReader.isBoolean(
                            value) : "Invalid UseCleanSession vaue: " + value;
                    safariOptions.useCleanSession(valueOf(value));
                    break;
                case USE_TECHNOLOGY_PREVIEW:
                    assert annotationsReader.isBoolean(
                            value) : "Invalid UseTechnologyPreview value: "
                                    + value;
                    safariOptions.setUseTechnologyPreview(valueOf(value));
                    break;
                default:
                    log.warn("Option {} not supported for Edge", name);
                }
            }
        } else {
            // If not, search DriverOptions in any field
            Object optionsFromAnnotatedField = annotationsReader
                    .getOptionsFromAnnotatedField(testInstance,
                            DriverOptions.class);
            if (optionsFromAnnotatedField != null) {
                safariOptions = (SafariOptions) optionsFromAnnotatedField;
            }
        }
        return safariOptions;
    }

}
