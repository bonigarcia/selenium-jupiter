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
package io.github.bonigarcia;

import static io.github.bonigarcia.SeleniumJupiter.ARGS;
import static io.github.bonigarcia.SeleniumJupiter.BINARY;
import static io.github.bonigarcia.SeleniumJupiter.EXTENSIONS;
import static io.github.bonigarcia.SeleniumJupiter.EXTENSION_FILES;
import static io.github.bonigarcia.SeleniumJupiter.PAGE_LOAD_STRATEGY;
import static io.github.bonigarcia.SeleniumJupiter.USE_CLEAN_SESSION;
import static io.github.bonigarcia.SeleniumJupiter.USE_TECHNOLOGY_PREVIEW;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Options/capabilities parser (from annotated parameters to the proper type).
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class AnnotationsReader {

    protected static final Logger log = LoggerFactory
            .getLogger(AnnotationsReader.class);

    protected ChromeOptions getChromeOptions(Parameter parameter,
            Optional<Object> testInstance) {
        ChromeOptions chromeOptions = new ChromeOptions();
        Class<DriverOptions> driverOptionsClass = DriverOptions.class;
        DriverOptions driverOptions = parameter
                .getAnnotation(driverOptionsClass);

        // Search first DriverOptions annotation in parameter
        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();
                switch (name) {
                case ARGS:
                    chromeOptions.addArguments(value);
                    break;
                case BINARY:
                    chromeOptions.setBinary(value);
                    break;
                case EXTENSIONS:
                    chromeOptions.addEncodedExtensions(value);
                    break;
                case EXTENSION_FILES:
                    chromeOptions.addExtensions(new File(value));
                    break;
                default:
                    chromeOptions.setExperimentalOption(name, value);
                }
            }
        } else {
            // If not, search DriverOptions in any field
            Optional<Object> annotatedField = seekFieldAnnotatedWith(
                    testInstance, driverOptionsClass);
            if (annotatedField.isPresent()) {
                chromeOptions = (ChromeOptions) annotatedField.get();
            }
        }

        return chromeOptions;
    }

    protected FirefoxOptions getFirefoxOptions(Parameter parameter,
            Optional<Object> testInstance) {
        Class<DriverOptions> driverOptionsClass = DriverOptions.class;
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        DriverOptions driverOptions = parameter
                .getAnnotation(driverOptionsClass);

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
                    if (isBoolean(value)) {
                        firefoxOptions.addPreference(name,
                                Boolean.valueOf(value));
                    } else if (isNumeric(value)) {
                        firefoxOptions.addPreference(name,
                                Integer.parseInt(value));
                    } else {
                        firefoxOptions.addPreference(name, value);
                    }
                }
            }
        } else {
            // If not, search DriverOptions in any field
            Optional<Object> annotatedField = seekFieldAnnotatedWith(
                    testInstance, driverOptionsClass);
            if (annotatedField.isPresent()) {
                firefoxOptions = (FirefoxOptions) annotatedField.get();
            }
        }
        return firefoxOptions;
    }

    protected EdgeOptions getEdgeOptions(Parameter parameter) {
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);
        EdgeOptions edgeOptions = new EdgeOptions();
        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();
                switch (name) {
                case PAGE_LOAD_STRATEGY:
                    edgeOptions.setPageLoadStrategy(value);
                    break;
                default:
                    log.warn("Option {} not supported for Edge", name);
                }
            }
        }
        return edgeOptions;
    }

    protected OperaOptions getOperaOptions(Parameter parameter) {
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);
        OperaOptions operaOptions = new OperaOptions();
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
        }
        return operaOptions;
    }

    protected SafariOptions getSafariOptions(Parameter parameter) {
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);
        SafariOptions safariOptions = new SafariOptions();
        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();
                switch (name) {
                case PAGE_LOAD_STRATEGY:
                    if (isNumeric(value)) {
                        safariOptions.setPort(Integer.parseInt(value));
                    } else {
                        log.warn("Port {} not valid for Safari options", value);
                    }
                    break;

                case USE_CLEAN_SESSION:
                    if (isBoolean(value)) {
                        safariOptions
                                .setUseCleanSession(Boolean.valueOf(value));
                    } else {
                        log.warn(
                                "UseCleanSession {} not valid for Safari options",
                                value);
                    }
                    break;

                case USE_TECHNOLOGY_PREVIEW:
                    if (isBoolean(value)) {
                        safariOptions.setUseTechnologyPreview(
                                Boolean.valueOf(value));
                    } else {
                        log.warn(
                                "UseTechnologyPreview {} not valid for Safari options",
                                value);
                    }
                    break;

                default:
                    log.warn("Option {} not supported for Edge", name);
                }
            }
        }
        return safariOptions;
    }

    protected Optional<Capabilities> getCapabilities(Parameter parameter,
            Optional<Object> testInstance) {
        Optional<Capabilities> out = Optional.empty();
        Class<DriverCapabilities> driverCapabilititesClass = DriverCapabilities.class;
        DriverCapabilities driverCapabilities = parameter
                .getAnnotation(driverCapabilititesClass);

        Capabilities capabilities = null;
        if (driverCapabilities != null) {
            // Search first DriverCapabilities annotation in parameter
            capabilities = new DesiredCapabilities();
            for (Capability capability : driverCapabilities.capability()) {
                ((DesiredCapabilities) capabilities)
                        .setCapability(capability.name(), capability.value());
            }
            out = Optional.of(capabilities);
        } else {
            // If not, search DriverCapabilities in any field
            Optional<Object> annotatedField = seekFieldAnnotatedWith(
                    testInstance, driverCapabilititesClass);
            if (annotatedField.isPresent()) {
                capabilities = (Capabilities) annotatedField.get();
                out = Optional.of(capabilities);
            }
        }
        return out;
    }

    protected Optional<URL> getUrl(Parameter parameter,
            Optional<Object> testInstance) {
        Optional<URL> out = Optional.empty();
        Class<DriverUrl> driverUrlClass = DriverUrl.class;

        String urlValue = null;
        try {

            DriverUrl driverUrl = parameter.getAnnotation(driverUrlClass);
            if (driverUrl != null) {
                // Search first DriverUrl annotation in parameter
                urlValue = driverUrl.value();
                out = Optional.of(new URL(urlValue));
            } else {
                // If not, search DriverUrl in any field
                Optional<Object> annotatedField = seekFieldAnnotatedWith(
                        testInstance, driverUrlClass);
                if (annotatedField.isPresent()) {
                    urlValue = (String) annotatedField.get();
                    out = Optional.of(new URL(urlValue));
                }
            }
        } catch (MalformedURLException e) {
            log.warn("Bad URL {}", urlValue, e);
        }

        return out;
    }

    private boolean isBoolean(String s) {
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false");
    }

    private boolean isNumeric(String s) {
        return StringUtils.isNumeric(s);
    }

    private Optional<Object> seekFieldAnnotatedWith(
            Optional<Object> testInstance,
            Class<? extends Annotation> annotation) {
        Optional<Object> out = Optional.empty();
        if (testInstance.isPresent()) {
            Object object = testInstance.get();
            Field[] declaredFields = object.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(annotation)) {
                    try {
                        field.setAccessible(true);
                        out = Optional.of(field.get(object));
                    } catch (Exception e) {
                        log.warn(
                                "Exception searching annotation {} in test instance {}",
                                annotation, testInstance, e);
                    }
                }
            }
        }
        return out;
    }

}
