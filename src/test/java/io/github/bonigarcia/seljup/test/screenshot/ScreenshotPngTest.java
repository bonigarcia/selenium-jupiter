/*
 * (C) Copyright 2017 Boni Garcia (https://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup.test.screenshot;

//tag::snippet-in-doc[]
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.SessionId;

import io.github.bonigarcia.seljup.SeleniumJupiter;

class ScreenshotPngTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    SessionId sessionId;

    @BeforeAll
    static void setup() {
        seleniumJupiter.getConfig().enableScreenshot();
        seleniumJupiter.getConfig().takeScreenshotAsPng(); // Default option
    }

    @AfterEach
    void teardown() {
        File[] files = new File(".").listFiles();
        Optional<File> screenshot = Arrays.stream(files).filter(
                f -> f.getName().endsWith(sessionId.toString() + ".png"))
                .findFirst();
        assertThat(screenshot).isNotEmpty();
        screenshot.get().delete();
    }

    @Test
    void screenshotTest(ChromeDriver driver) {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.getTitle()).contains("Selenium WebDriver");

        sessionId = driver.getSessionId();
    }

}
//end::snippet-in-doc[]
