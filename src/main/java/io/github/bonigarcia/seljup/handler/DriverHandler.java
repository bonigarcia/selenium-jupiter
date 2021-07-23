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
package io.github.bonigarcia.seljup.handler;

import static java.io.File.createTempFile;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.BrowserType;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Abstract driver handler.
 *
 * @author Boni Garcia
 * @since 1.2.0
 */
public abstract class DriverHandler {

    static final Logger log = getLogger(lookup().lookupClass());

    protected Config config;
    protected AnnotationsReader annotationsReader;
    protected Parameter parameter;
    protected ExtensionContext extensionContext;

    protected DriverHandler(Parameter parameter,
            ExtensionContext extensionContext, Config config,
            AnnotationsReader annotationsReader) {
        this.parameter = parameter;
        this.extensionContext = extensionContext;
        this.config = config;
        this.annotationsReader = annotationsReader;
    }

    public static Optional<DriverHandler> getInstance(BrowserType browserType,
            Parameter parameter, ExtensionContext extensionContext,
            Config config, AnnotationsReader annotationsReader) {
        Class<?> type = parameter.getType();
        if (type == ChromeDriver.class
                || (browserType != null && (browserType == BrowserType.CHROME
                        || browserType == BrowserType.CHROME_MOBILE))) {
            return Optional.of(new ChromeDriverHandler(parameter, extensionContext, config,
                    annotationsReader));
        } else if (type == FirefoxDriver.class || (browserType != null
                && browserType == BrowserType.FIREFOX)) {
            return Optional.of(new FirefoxDriverHandler(parameter, extensionContext, config,
                    annotationsReader));
        } else if (type == OperaDriver.class
                || (browserType != null && browserType == BrowserType.OPERA)) {
            return Optional.of(new OperaDriverHandler(parameter, extensionContext, config,
                    annotationsReader));
        } else if (type == EdgeDriver.class
                || (browserType != null && browserType == BrowserType.EDGE)) {
            return Optional.of(new EdgeDriverHandler(parameter, extensionContext, config,
                    annotationsReader));
        } else if (type == SafariDriver.class
                || (browserType != null && browserType == BrowserType.SAFARI)) {
            return Optional.of(new SafariDriverHandler(parameter, extensionContext, config,
                    annotationsReader));
        } else if (type == InternetExplorerDriver.class) {
            return Optional.of(new SafariDriverHandler(parameter, extensionContext, config,
                    annotationsReader));
        }
        return Optional.empty();
    }

    public abstract Capabilities getOptions(Parameter parameter,
            Optional<Object> testInstance);

    public Capabilities getCapabilities() {
        Optional<Object> testInstance = extensionContext.getTestInstance();
        Optional<Capabilities> capabilities = annotationsReader
                .getCapabilities(parameter, testInstance);
        Capabilities options = getOptions(parameter, testInstance);

        if (capabilities.isPresent()) {
            options.merge(capabilities.get());
        }

        return options;
    }

//    public String getName() {
//        String name = "";
//        Optional<Method> testMethod = context.getTestMethod();
//        if (testMethod.isPresent()) {
//            name = testMethod.get().getName() + "_";
//        } else {
//            Optional<Class<?>> testClass = context.getTestClass();
//            if (testClass.isPresent()) {
//                name = testClass.get().getSimpleName() + "_";
//            }
//        }
//        name += parameter.getName() + "_" + object.getClass().getSimpleName();
//        if (RemoteWebDriver.class.isAssignableFrom(object.getClass())) {
//            name += "_" + ((RemoteWebDriver) object).getSessionId();
//        }
//        return name;
//    }

    public File getExtension(String fileName) {
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
