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

import static java.lang.Boolean.valueOf;
import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

import io.github.bonigarcia.Option;
import io.github.bonigarcia.Option.Options;

/**
 * Resolver for FirefoxDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class FirefoxDriverHandler extends DriverHandler {

    public FirefoxDriverHandler() {
        super();
    }

    public FirefoxDriverHandler(Parameter parameter,
            Optional<Object> testInstance) {
        super(parameter, testInstance);
    }

    @Override
    public WebDriver resolve() {
        FirefoxDriver driver = null;
        try {
            Optional<Capabilities> capabilities = annotationsReader
                    .getCapabilities(parameter, testInstance);
            FirefoxOptions firefoxOptions = (FirefoxOptions) getOptions(
                    parameter, testInstance);
            if (capabilities.isPresent()) {
                firefoxOptions.merge(capabilities.get());
            }
            driver = new FirefoxDriver(firefoxOptions);
        } catch (Exception e) {
            handleException(e);
        }
        return driver;
    }

    @Override
    public MutableCapabilities getOptions(Parameter parameter,
            Optional<Object> testInstance)
            throws IOException, IllegalAccessException {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
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
                    firefoxOptions.addArguments(value);
                    break;
                case BINARY:
                    firefoxOptions.setBinary(value);
                    break;
                case EXTENSION:
                    FirefoxProfile firefoxProfile = new FirefoxProfile();
                    firefoxProfile.addExtension(getExtension(value));
                    firefoxOptions.setProfile(firefoxProfile);
                    break;
                case PREFS:
                    String name = option.name();
                    if (annotationsReader.isBoolean(value)) {
                        firefoxOptions.addPreference(name, valueOf(value));
                    } else if (annotationsReader.isNumeric(value)) {
                        firefoxOptions.addPreference(name, parseInt(value));
                    } else {
                        firefoxOptions.addPreference(name, value);
                    }
                    break;
                default:
                    log.warn("Option {} not supported for Firefox", type);
                }

            }
        } else {
            // If not, search options in any field
            Object optionsFromAnnotatedField = annotationsReader
                    .getOptionsFromAnnotatedField(testInstance, Options.class);
            if (optionsFromAnnotatedField != null) {
                firefoxOptions = (FirefoxOptions) optionsFromAnnotatedField;
            }
        }
        return firefoxOptions;
    }

}
