/*
 * (C) Copyright 2018 Boni Garcia (https://bonigarcia.github.io/)
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

import static io.github.bonigarcia.seljup.OutputHandler.BASE64_AND_PNG_KEY;
import static io.github.bonigarcia.seljup.OutputHandler.BASE64_KEY;
import static io.github.bonigarcia.seljup.OutputHandler.PNG_KEY;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.openqa.selenium.OutputType.BASE64;
import static org.openqa.selenium.OutputType.FILE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.config.Config;

/**
 * Screenshots handler.
 *
 * @author Boni Garcia
 * @since 2.0.0
 */
public class ScreenshotManager {

    final Logger log = getLogger(lookup().lookupClass());

    ExtensionContext extensionContext;
    Config config;
    OutputHandler outputHandler;

    public ScreenshotManager(ExtensionContext extensionContext, Config config,
            OutputHandler outputHandler) {
        this.extensionContext = extensionContext;
        this.config = config;
        this.outputHandler = outputHandler;
    }

    boolean isScreenshotRequired() {
        Optional<Throwable> executionException = extensionContext
                .getExecutionException();
        boolean isSscreenshot = config.isScreenshot();
        boolean isSscreenshotWhenFailure = config.isScreenshotWhenFailure();
        return isSscreenshot
                || (executionException.isPresent() && isSscreenshotWhenFailure);
    }

    void makeScreenshotIfRequired(List<WebDriver> driverList) {
        driverList.forEach(this::makeScreenshotIfRequired);
    }

    void makeScreenshotIfRequired(WebDriver driver) {
        if (isScreenshotRequired() && driver != null) {
            String screenshotFormat = config.getScreenshotFormat();
            switch (screenshotFormat) {
            case PNG_KEY:
                logFileScreenshot(driver);
                break;
            case BASE64_KEY:
                logBase64Screenshot(driver);
                break;
            case BASE64_AND_PNG_KEY:
                logBase64Screenshot(driver);
                logFileScreenshot(driver);
                break;
            default:
                log.warn("Invalid screenshot format {}", screenshotFormat);
                break;
            }
        }
    }

    void logBase64Screenshot(WebDriver driver) {
        try {
            String screenshotBase64 = ((TakesScreenshot) driver)
                    .getScreenshotAs(BASE64);
            log.debug("Screenshot (in Base64) at the end of test "
                    + "(copy&paste this string as URL in browser to watch it):\r\n"
                    + "data:image/png;base64,{}", screenshotBase64);
        } catch (Exception e) {
            log.trace("Exception getting screenshot in Base64", e);
        }
    }

    void logFileScreenshot(WebDriver driver) {
        try {
            File screenshotFile = ((TakesScreenshot) driver)
                    .getScreenshotAs(FILE);
            File destFile = outputHandler.getScreenshotFile(driver);
            log.trace("Creating screenshot for {} in {}", driver, destFile);
            copyFile(screenshotFile, destFile);

        } catch (Exception e) {
            log.trace("Exception getting screenshot as file", e);
        }
    }

}
