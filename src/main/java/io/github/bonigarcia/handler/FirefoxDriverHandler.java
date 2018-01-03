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
import static java.util.Arrays.stream;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

import io.github.bonigarcia.Arguments;
import io.github.bonigarcia.Binary;
import io.github.bonigarcia.Extensions;
import io.github.bonigarcia.Options;
import io.github.bonigarcia.Preferences;

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

        // @Arguments
        Arguments arguments = parameter.getAnnotation(Arguments.class);
        if (arguments != null) {
            stream(arguments.value()).forEach(firefoxOptions::addArguments);
        }

        // @Extensions
        Extensions extensions = parameter.getAnnotation(Extensions.class);
        if (extensions != null) {
            for (String extension : extensions.value()) {
                FirefoxProfile firefoxProfile = new FirefoxProfile();
                firefoxProfile.addExtension(getExtension(extension));
                firefoxOptions.setProfile(firefoxProfile);
            }
        }

        // @Binary
        Binary binary = parameter.getAnnotation(Binary.class);
        if (binary != null) {
            firefoxOptions.setBinary(binary.value());
        }

        // @Preferences
        managePreferences(parameter, testInstance, firefoxOptions);

        // @Options
        Object optionsFromAnnotatedField = annotationsReader
                .getOptionsFromAnnotatedField(testInstance, Options.class);
        if (optionsFromAnnotatedField != null) {
            firefoxOptions = ((FirefoxOptions) optionsFromAnnotatedField)
                    .merge(firefoxOptions);
        }

        return firefoxOptions;
    }

    private void managePreferences(Parameter parameter,
            Optional<Object> testInstance, FirefoxOptions firefoxOptions) {
        Preferences preferences = parameter.getAnnotation(Preferences.class);
        if (preferences != null) {
            for (String preference : preferences.value()) {
                Optional<List<String>> keyValue = annotationsReader
                        .getKeyValue(preference);
                if (!keyValue.isPresent()) {
                    continue;
                }
                String name = keyValue.get().get(0);
                String value = keyValue.get().get(1);
                if (annotationsReader.isBoolean(value)) {
                    firefoxOptions.addPreference(name, valueOf(value));
                } else if (annotationsReader.isNumeric(value)) {
                    firefoxOptions.addPreference(name, parseInt(value));
                } else {
                    firefoxOptions.addPreference(name, value);
                }
            }
        }
    }

}
