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
package io.github.bonigarcia;

import static io.github.bonigarcia.SeleniumJupiter.getOutputFolder;
import static io.github.bonigarcia.SeleniumJupiter.getString;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.openqa.selenium.OutputType.BASE64;
import static org.openqa.selenium.OutputType.FILE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

/**
 * Utilities for screenshots.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class ScreenshotManager {

    final Logger log = getLogger(lookup().lookupClass());

    ExtensionContext context;

    public ScreenshotManager(ExtensionContext context) {
        this.context = context;
    }

    boolean isScreenshotRequired() {
        Optional<Throwable> executionException = context
                .getExecutionException();
        String screenshotAtTheEnd = getString(
                "sel.jup.screenshot.at.the.end.of.tests");

        return screenshotAtTheEnd.equalsIgnoreCase("true")
                || (executionException.isPresent()
                        && screenshotAtTheEnd.equalsIgnoreCase("whenfailure"));
    }

    void makeScreenshot(WebDriver driver, String fileName) {
        if (isScreenshotRequired() && driver != null) {
            String screenshotFormat = getString("sel.jup.screenshot.format");
            switch (screenshotFormat) {
            case "png":
                logFileScreenshot(driver, fileName);
                break;
            case "base64":
                logBase64Screenshot(driver, fileName);
                break;
            case "base64andpng":
                logBase64Screenshot(driver, fileName);
                logFileScreenshot(driver, fileName);
                break;
            default:
                log.warn("Invalid screenshot format {}", screenshotFormat);
                break;
            }
        }
    }

    void logBase64Screenshot(WebDriver driver, String fileName) {
        try {
            String screenshotBase64 = ((TakesScreenshot) driver)
                    .getScreenshotAs(BASE64);
            log.info("Screenshot (in Base64) at the end of {} "
                    + "(copy&paste this string as URL in browser to watch it):\r\n"
                    + "data:image/png;base64,{}", fileName, screenshotBase64);
        } catch (Exception e) {
            log.trace("Exception getting screenshot in Base64", e);
        }
    }

    void logFileScreenshot(WebDriver driver, String fileName) {
        try {
            File screenshotFile = ((TakesScreenshot) driver)
                    .getScreenshotAs(FILE);
            String outputFolder = getOutputFolder(context);
            copyFile(screenshotFile, new File(outputFolder, fileName + ".png"));

        } catch (Exception e) {
            log.trace("Exception getting screenshot as file", e);
        }
    }
}
