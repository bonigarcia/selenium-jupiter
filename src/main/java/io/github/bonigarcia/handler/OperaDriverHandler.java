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

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;

import io.github.bonigarcia.Option;
import io.github.bonigarcia.Option.Options;

/**
 * Resolver for OperaDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class OperaDriverHandler extends DriverHandler {

    public OperaDriverHandler() {
        super();
    }

    public OperaDriverHandler(Parameter parameter,
            Optional<Object> testInstance) {
        super(parameter, testInstance);
    }

    @Override
    public WebDriver resolve() {
        OperaDriver driver = null;
        try {
            Optional<Capabilities> capabilities = annotationsReader
                    .getCapabilities(parameter, testInstance);
            OperaOptions operaOptions = (OperaOptions) getOptions(parameter,
                    testInstance);
            if (capabilities.isPresent()) {
                operaOptions.merge(capabilities.get());
            }
            driver = new OperaDriver(operaOptions);
        } catch (Exception e) {
            handleException(e);
        }
        return driver;
    }

    @Override
    public MutableCapabilities getOptions(Parameter parameter,
            Optional<Object> testInstance)
            throws IOException, IllegalAccessException {
        OperaOptions operaOptions = new OperaOptions();
        Option[] optionArr = parameter.getAnnotationsByType(Option.class);
        Options options = parameter.getAnnotation(Options.class);
        Option[] allOptions = options != null ? options.value() : optionArr;

        // Search first options annotation in parameter
        if (allOptions.length > 0) {
            for (Option option : allOptions) {
                Option.Type type = option.type();
                String value = option.value();
                switch (type) {
                case ARGS:
                    operaOptions.addArguments(value);
                    break;
                case BINARY:
                    operaOptions.setBinary(value);
                    break;
                case EXTENSION:
                    operaOptions.addExtensions(getExtension(value));
                    break;
                default:
                    log.warn("Option {} not supported for Opera", type);
                }

            }
        } else {
            // If not, search options in any field
            Object optionsFromAnnotatedField = annotationsReader
                    .getOptionsFromAnnotatedField(testInstance, Options.class);
            if (optionsFromAnnotatedField != null) {
                operaOptions = (OperaOptions) optionsFromAnnotatedField;
            }
        }
        return operaOptions;
    }

}
