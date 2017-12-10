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

import static io.github.bonigarcia.SeleniumJupiter.ARGS;
import static io.github.bonigarcia.SeleniumJupiter.BINARY;
import static java.lang.Boolean.valueOf;
import static java.lang.Integer.parseInt;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;

import io.github.bonigarcia.AnnotationsReader;
import io.github.bonigarcia.DriverOptions;
import io.github.bonigarcia.Option;

/**
 * Resolver for FirefoxDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class FirefoxDriverHandler extends AbstractDriverHandler {

    final Logger log = getLogger(lookup().lookupClass());

    static FirefoxDriverHandler instance;

    public static synchronized FirefoxDriverHandler getInstance() {
        if (instance == null) {
            instance = new FirefoxDriverHandler();
        }
        return instance;
    }

    public WebDriver resolve(Parameter parameter,
            Optional<Object> testInstance) {
        FirefoxDriver driver = null;
        try {
            Optional<Capabilities> capabilities = AnnotationsReader
                    .getInstance().getCapabilities(parameter, testInstance);
            FirefoxOptions firefoxOptions = getFirefoxOptions(parameter,
                    testInstance);
            if (capabilities.isPresent()) {
                firefoxOptions.merge(capabilities.get());
            }
            driver = new FirefoxDriver(firefoxOptions);
        } catch (Exception e) {
            handleException(e);
        }
        return driver;
    }

    public FirefoxOptions getFirefoxOptions(Parameter parameter,
            Optional<Object> testInstance) {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);

        // Search first DriverOptions annotation in parameter
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
                    if (AnnotationsReader.getInstance().isBoolean(value)) {
                        firefoxOptions.addPreference(name, valueOf(value));
                    } else if (AnnotationsReader.getInstance()
                            .isNumeric(value)) {
                        firefoxOptions.addPreference(name, parseInt(value));
                    } else {
                        firefoxOptions.addPreference(name, value);
                    }
                }
            }
        } else {
            // If not, search DriverOptions in any field
            Object optionsFromAnnotatedField = AnnotationsReader.getInstance()
                    .getOptionsFromAnnotatedField(testInstance,
                            DriverOptions.class);
            if (optionsFromAnnotatedField != null) {
                firefoxOptions = (FirefoxOptions) optionsFromAnnotatedField;
            }
        }
        return firefoxOptions;
    }

}
