/*
 * (C) Copyright 2018 Boni Garcia (https://bonigarcia.github.io/)
 * (C) Copyright 2022 Bosch.IO GmbH
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

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Locale.ROOT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.config.Config;

/**
 * Logic output folder and files.
 *
 * @author Boni Garcia
 * @since 2.1.0
 */
public class OutputHandler {

    static final Logger log = getLogger(lookup().lookupClass());

    public static final String SEPARATOR = "_";
    public static final String DATE_FORMAT = "yyyy.MM.dd_HH.mm.ss.SSS";
    public static final String BASE64_KEY = "base64";
    public static final String PNG_KEY = "png";
    public static final String BASE64_AND_PNG_KEY = "base64andpng";
    public static final String SUREFIRE_REPORTS_KEY = "surefire-reports";
    public static final String SUREFIRE_REPORTS_FOLDER = "./target/surefire-reports/";

    ExtensionContext extensionContext;
    Config config;
    Parameter parameter;

    public OutputHandler(ExtensionContext extensionContext, Config config,
            Parameter parameter) {
        this.extensionContext = extensionContext;
        this.config = config;
        this.parameter = parameter;
    }

    public File getScreenshotFile(WebDriver driver) {
        String outputFolder = getOutputFolder();
        String fileName = getOutputFileName(driver);
        return new File(outputFolder, fileName + "." + PNG_KEY);
    }

    public String getPrefix() {
        String prefix = "";
        Optional<Method> testMethod = extensionContext.getTestMethod();
        if (testMethod.isPresent()) {
            prefix = testMethod.get().getName() + SEPARATOR;
        } else {
            Optional<Class<?>> testClass = extensionContext.getTestClass();
            if (testClass.isPresent()) {
                prefix = testClass.get().getSimpleName() + SEPARATOR;
            }
        }
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        prefix += parameter.getName() + SEPARATOR + dateFormat.format(now)
                + SEPARATOR;
        return prefix;
    }

    public String getOutputFileName(WebDriver driver) {
        String name = getPrefix();
        Class<? extends WebDriver> driverClass = driver.getClass();
        if (RemoteWebDriver.class.isAssignableFrom(driverClass)) {
            name += ((RemoteWebDriver) driver).getCapabilities()
                    .getBrowserName().toLowerCase(ROOT) + SEPARATOR
                    + ((RemoteWebDriver) driver).getSessionId();
        }
        return name;
    }

    public String getOutputFolder() {
        String outputFolder = config.getOutputFolder();
        Optional<Method> testMethod = extensionContext.getTestMethod();
        Optional<Class<?>> testInstance = extensionContext.getTestClass();
        if (testMethod.isPresent() && testInstance.isPresent()) {
            Class<?> testClass = testInstance.get();
            if (outputFolder.equalsIgnoreCase(SUREFIRE_REPORTS_KEY)) {
                // backwards-compatibility: if the surefire-key is configured, always use class-specific output folder
                outputFolder = getClassSpecificOutputFolder(SUREFIRE_REPORTS_FOLDER, testClass);
            } else {
                if (outputFolder.isEmpty()) {
                    outputFolder = ".";
                }

                if (config.isOutputFolderPerClass()) {
                    outputFolder = getClassSpecificOutputFolder(outputFolder, testClass);
                }
            }
        }

        log.trace("Output folder {}", outputFolder);
        File outputFolderFile = new File(outputFolder);
        if (!outputFolderFile.exists()) {
            outputFolderFile.mkdirs();
        }
        return outputFolder;
    }

    private String getClassSpecificOutputFolder(String baseFolder, Class<?> testClass) {
        String testClassName = testClass.getName();

        StringBuilder stringBuilder = new StringBuilder(
                baseFolder);
        if (!baseFolder.endsWith(File.separator)) {
            stringBuilder.append(File.separator);
        }
        stringBuilder.append(testClassName);

        return stringBuilder.toString();
    }

}
