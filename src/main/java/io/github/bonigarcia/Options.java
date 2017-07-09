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
 * Options parser (from annotated parameters to the proper type).
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class Options {

    // Chrome, Firefox, Opera
    public static final String ARGS = "args";
    public static final String BINARY = "binary";

    // Chrome, Opera
    public static final String EXTENSIONS = "extensions";
    public static final String EXTENSION_FILES = "extensionFiles";

    // Edge
    public static final String PAGE_LOAD_STRATEGY = "pageLoadStrategy";

    // Safari
    public static final String PORT = "port";
    public static final String USE_CLEAN_SESSION = "useCleanSession";
    public static final String USE_TECHNOLOGY_PREVIEW = "useTechnologyPreview";

    protected static final Logger log = LoggerFactory.getLogger(Options.class);

    protected ChromeOptions getChromeOptions(Parameter parameter) {
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);
        ChromeOptions chromeOptions = new ChromeOptions();
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
        }
        return chromeOptions;
    }

    protected FirefoxOptions getFirefoxOptions(Parameter parameter) {
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);
        FirefoxOptions firefoxOptions = new FirefoxOptions();
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

    protected Capabilities getCapabilities(Parameter parameter) {
        DriverCapabilities driverCapabilities = parameter
                .getAnnotation(DriverCapabilities.class);
        Capabilities capabilities = new DesiredCapabilities();

        if (driverCapabilities != null) {
            for (Capability capability : driverCapabilities.capability()) {
                ((DesiredCapabilities) capabilities)
                        .setCapability(capability.name(), capability.value());
            }
        }
        return capabilities;
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
