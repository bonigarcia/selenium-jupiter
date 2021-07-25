/*
 * (C) Copyright 2021 Boni Garcia (http://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup;

import static java.io.File.createTempFile;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Locale.ROOT;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.chromium.ChromiumOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;

import com.google.gson.internal.LinkedTreeMap;

import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import io.github.bonigarcia.seljup.config.Config;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Driver handler.
 *
 * @author Boni Garcia
 * @since 4.0.0
 */
public class CapabilitiesHandler {

    static final Logger log = getLogger(lookup().lookupClass());

    Config config;
    AnnotationsReader annotationsReader;
    Parameter parameter;
    ExtensionContext extensionContext;
    Optional<Browser> browser;
    Optional<BrowserType> browserType;
    boolean isGeneric;

    public CapabilitiesHandler(Config config,
            AnnotationsReader annotationsReader, Parameter parameter,
            ExtensionContext extensionContext, Optional<Browser> browser,
            Optional<BrowserType> browserType, boolean isGeneric) {
        this.config = config;
        this.annotationsReader = annotationsReader;
        this.parameter = parameter;
        this.extensionContext = extensionContext;
        this.browser = browser;
        this.browserType = browserType;
        this.isGeneric = isGeneric;
    }

    public Optional<Capabilities> getCapabilities() {
        Optional<Class<? extends Capabilities>> optionsClass = getOptionsClass();
        if (optionsClass.isPresent()) {
            Capabilities options = getOptions(optionsClass.get());
            if (options != null) {
                Optional<Object> testInstance = extensionContext
                        .getTestInstance();
                Optional<Capabilities> capabilities = annotationsReader
                        .getCapabilities(parameter, testInstance);
                if (capabilities.isPresent()) {
                    options.merge(capabilities.get());
                }
                return Optional.of(options);
            }
        }
        return Optional.empty();
    }

    private Optional<Class<? extends Capabilities>> getOptionsClass() {
        Class<?> type = parameter.getType();
        if (isGeneric) {
            String defaultBrowser = WebDriverManager.getInstance().config()
                    .getDefaultBrowser();
            browserType = Optional.of(Browser.toBrowserType(defaultBrowser));
        }
        log.trace("Getting capabilities for type={} -- browserType={}", type,
                browserType);

        if (type == ChromeDriver.class || (browserType.isPresent()
                && browserType.get().isChromeBased())) {
            return Optional.of(ChromeOptions.class);
        } else if (type == FirefoxDriver.class || (browserType.isPresent()
                && browserType.get() == BrowserType.FIREFOX)) {
            return Optional.of(FirefoxOptions.class);
        } else if (type == OperaDriver.class || (browserType.isPresent()
                && browserType.get() == BrowserType.OPERA)) {
            return Optional.of(OperaOptions.class);
        } else if (type == EdgeDriver.class || (browserType.isPresent()
                && browserType.get() == BrowserType.EDGE)) {
            return Optional.of(EdgeOptions.class);
        } else if (type == SafariDriver.class || (browserType.isPresent()
                && browserType.get() == BrowserType.SAFARI)) {
            return Optional.of(SafariOptions.class);
        } else if (type == InternetExplorerDriver.class) {
            return Optional.of(InternetExplorerOptions.class);
        } else if (type == ChromiumDriver.class) {
            return Optional.of(ChromiumOptions.class);
        }

        return Optional.empty();
    }

    private Capabilities getOptions(
            Class<? extends Capabilities> optionsClass) {
        Optional<Object> testInstance = extensionContext.getTestInstance();

        Capabilities options = null;
        try {
            options = optionsClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.warn("Exception creating instance of {}", optionsClass);
            return options;
        }

        log.trace("Getting options for {}", optionsClass);

        // Arguments
        handleArguments(optionsClass, options);

        // Extensions
        handleExtensions(optionsClass, options);

        // Preferences
        handlePreferences(optionsClass, options);

        // Binary
        handleBinary(optionsClass, options);

        // Options
        options = handleOptions(optionsClass, testInstance, options);

        // Capabilities
        handleCapabilities(optionsClass, options);

        return options;

    }

    private void handleCapabilities(Class<? extends Capabilities> optionsClass,
            Capabilities options) {
        try {
            if (browser.isPresent() && browser.get() != null
                    && browser.get().getCapabilities() != null) {
                Method setCapabilityMethod = optionsClass
                        .getMethod("setCapability", String.class, String.class);

                @SuppressWarnings("unchecked")
                Set<Entry<String, String>> caps = ((LinkedTreeMap<String, String>) browser
                        .get().getCapabilities()).entrySet();
                for (Entry<String, String> entry : caps) {
                    setCapabilityMethod.invoke(options, entry.getKey(),
                            entry.getValue());
                }
            }
        } catch (Exception e) {
            log.trace("Exception reading capabilities of {} ({})", optionsClass,
                    e.getMessage());
        }
    }

    private Capabilities handleOptions(
            Class<? extends Capabilities> optionsClass,
            Optional<Object> testInstance, Capabilities options) {
        try {
            Capabilities optionsFromAnnotatedField = annotationsReader
                    .getFromAnnotatedField(testInstance, Options.class,
                            optionsClass);
            if (optionsFromAnnotatedField != null) {
                options = optionsFromAnnotatedField.merge(options);
            }
        } catch (Exception e) {
            log.trace("Exception reading options of {} ({})", optionsClass,
                    e.getMessage());
        }
        return options;
    }

    private void handleBinary(Class<? extends Capabilities> optionsClass,
            Capabilities options) {
        try {
            Method setBinaryMethod = optionsClass.getMethod("setBinary",
                    String.class);

            Binary binary = parameter.getAnnotation(Binary.class);
            if (binary != null) {
                setBinaryMethod.invoke(options, binary.value());
            }
        } catch (Exception e) {
            log.trace("Exception reading binary of {} ({})", optionsClass,
                    e.getMessage());
        }
    }

    private void handlePreferences(Class<? extends Capabilities> optionsClass,
            Capabilities options) {
        try {
            Method addPreferenceMethod = optionsClass.getMethod("addPreference",
                    String.class, Object.class);

            Preferences preferences = parameter
                    .getAnnotation(Preferences.class);
            if (preferences != null) {
                addPreferences(options, preferences.value(),
                        addPreferenceMethod);
            }
            if (browser.isPresent() && browser.get() != null
                    && browser.get().getPreferences() != null) {
                addPreferences(options, browser.get().getPreferences(),
                        addPreferenceMethod);
            }

        } catch (Exception e) {
            log.trace("Exception reading preferences of {} ({})", optionsClass,
                    e.getMessage());
        }
    }

    private void addPreferences(Capabilities options, String[] preferences,
            Method addPreferenceMethod)
            throws IllegalAccessException, InvocationTargetException {
        for (String preference : preferences) {
            Optional<List<Object>> keyValue = annotationsReader
                    .getKeyValue(preference);
            if (!keyValue.isPresent()) {
                continue;
            }
            String name = keyValue.get().get(0).toString();
            String value = keyValue.get().get(1).toString();
            if (annotationsReader.isBoolean(value)) {
                addPreferenceMethod.invoke(options, name,
                        Boolean.valueOf(value));
            } else if (annotationsReader.isNumeric(value)) {
                addPreferenceMethod.invoke(options, name,
                        Integer.parseInt(value));
            } else {
                addPreferenceMethod.invoke(options, name, value);
            }
        }
    }

    private void handleExtensions(Class<? extends Capabilities> optionsClass,
            Capabilities options) {
        try {
            boolean isFirefox = optionsClass == FirefoxOptions.class;
            Method addExtensionsMethod = isFirefox
                    ? optionsClass.getMethod("setProfile", FirefoxProfile.class)
                    : optionsClass.getMethod("addExtensions", File[].class);

            Extensions extensions = parameter.getAnnotation(Extensions.class);
            if (extensions != null) {
                for (String extension : extensions.value()) {
                    if (isFirefox) {
                        FirefoxProfile firefoxProfile = new FirefoxProfile();
                        firefoxProfile.addExtension(getExtension(extension));
                        addExtensionsMethod.invoke(options, firefoxProfile);
                    } else {
                        addExtensionsMethod.invoke(options,
                                getExtension(extension));
                    }
                }
            }

        } catch (Exception e) {
            log.trace("Exception reading extensions of {} ({})", optionsClass,
                    e.getMessage());
        }
    }

    private void handleArguments(Class<? extends Capabilities> optionsClass,
            Capabilities options) {
        try {
            Method addArgumentsMethod = optionsClass.getMethod("addArguments",
                    List.class);
            Arguments arguments = parameter.getAnnotation(Arguments.class);
            if (arguments != null) {
                addArgumentsMethod.invoke(options,
                        Arrays.asList(arguments.value()));
            }
            if (browser.isPresent() && browser.get() != null
                    && browser.get().getArguments() != null) {
                addArgumentsMethod.invoke(options,
                        Arrays.asList(browser.get().getArguments()));
            }
        } catch (Exception e) {
            log.trace("Exception reading arguments of {} ({})", optionsClass,
                    e.getMessage());
        }
    }

    private File getExtension(String fileName) {
        File file = new File(fileName);
        try {
            if (!file.exists()) {
                InputStream inputStream = this.getClass()
                        .getResourceAsStream("/" + file);
                if (inputStream != null) {
                    file = createTempFile("tmp-", fileName);
                    file.deleteOnExit();
                    copyInputStreamToFile(inputStream, file);
                }
            }
        } catch (Exception e) {
            log.warn("There was a problem handling extension", e);
        }
        return file;
    }

}
