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
import static io.github.bonigarcia.SeleniumJupiter.EXTENSIONS;
import static io.github.bonigarcia.SeleniumJupiter.EXTENSION_FILES;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;
import org.slf4j.Logger;

import io.github.bonigarcia.AnnotationsReader;
import io.github.bonigarcia.DriverOptions;
import io.github.bonigarcia.Option;

/**
 * Resolver for OperaDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class OperaDriverHandler {

    final Logger log = getLogger(lookup().lookupClass());

    static OperaDriverHandler instance;

    public static synchronized OperaDriverHandler getInstance() {
        if (instance == null) {
            instance = new OperaDriverHandler();
        }
        return instance;
    }

    public WebDriver resolve(Parameter parameter,
            Optional<Object> testInstance) {
        Optional<Capabilities> capabilities = AnnotationsReader.getInstance()
                .getCapabilities(parameter, testInstance);
        OperaOptions operaOptions = getOperaOptions(parameter, testInstance);
        if (capabilities.isPresent()) {
            operaOptions.merge(capabilities.get());
        }
        return new OperaDriver(operaOptions);
    }

    public OperaOptions getOperaOptions(Parameter parameter,
            Optional<Object> testInstance) {
        OperaOptions operaOptions = new OperaOptions();
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);

        // Search first DriverOptions annotation in parameter
        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();
                switch (name) {
                case ARGS:
                    operaOptions.addArguments(value);
                    break;
                case BINARY:
                    operaOptions.setBinary(value);
                    break;
                case EXTENSIONS:
                    operaOptions.addEncodedExtensions(value);
                    break;
                case EXTENSION_FILES:
                    operaOptions.addExtensions(new File(value));
                    break;
                default:
                    operaOptions.setExperimentalOption(name, value);
                }
            }
        } else {
            // If not, search DriverOptions in any field
            Object optionsFromAnnotatedField = AnnotationsReader.getInstance()
                    .getOptionsFromAnnotatedField(testInstance,
                            DriverOptions.class);
            if (optionsFromAnnotatedField != null) {
                operaOptions = (OperaOptions) optionsFromAnnotatedField;
            }
        }
        return operaOptions;
    }

}
