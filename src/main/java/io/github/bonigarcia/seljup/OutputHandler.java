/*
 * (C) Copyright 2018 Boni Garcia (http://bonigarcia.github.io/)
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
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.lang.reflect.Method;
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

    public static final String BASE64_KEY = "base64";
    public static final String PNG_KEY = "png";
    public static final String BASE64_AND_PNG_KEY = "base64andpng";
    public static final String SUREFIRE_REPORTS_KEY = "surefire-reports";
    public static final String SUREFIRE_REPORTS_FOLDER = "./target/surefire-reports/";

    ExtensionContext extensionContext;
    Config config;

    public OutputHandler(ExtensionContext extensionContext, Config config) {
        this.extensionContext = extensionContext;
        this.config = config;
    }

    public File getScreenshotFile(WebDriver driver) {
        String outputFolder = getOutputFolder();
        String fileName = getOutputFileName(driver);
        File destFile = new File(outputFolder, fileName + "." + PNG_KEY);
        return destFile;
    }

    public String getOutputFileName(WebDriver driver) {
        String name = "";
        Optional<Method> testMethod = extensionContext.getTestMethod();
        if (testMethod.isPresent()) {
            name = testMethod.get().getName() + "_";
        } else {
            Optional<Class<?>> testClass = extensionContext.getTestClass();
            if (testClass.isPresent()) {
                name = testClass.get().getSimpleName() + "_";
            }
        }
        name += driver.getClass().getSimpleName();
        if (RemoteWebDriver.class.isAssignableFrom(driver.getClass())) {
            name += "_" + ((RemoteWebDriver) driver).getSessionId();
        }
        return name;
    }

    public String getOutputFolder() {
        String outputFolder = config.getOutputFolder();
        Optional<Method> testMethod = extensionContext.getTestMethod();
        Optional<Class<?>> testInstance = extensionContext.getTestClass();
        if (testMethod.isPresent() && testInstance.isPresent()) {
            if (outputFolder.equalsIgnoreCase(SUREFIRE_REPORTS_KEY)) {
                outputFolder = getSurefireOutputFolder(testInstance.get());
            } else if (outputFolder.isEmpty()) {
                outputFolder = ".";
            }
        }

        log.trace("Output folder {}", outputFolder);
        File outputFolderFile = new File(outputFolder);
        if (!outputFolderFile.exists()) {
            outputFolderFile.mkdirs();
        }
        return outputFolder;
    }

    public String getSurefireOutputFolder(Class<?> testInstance) {
        StringBuilder stringBuilder = new StringBuilder(
                SUREFIRE_REPORTS_FOLDER);
        stringBuilder.append(testInstance.getName());
        return stringBuilder.toString();
    }

}
