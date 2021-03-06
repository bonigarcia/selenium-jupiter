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
package io.github.bonigarcia.seljup.test.screenshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;

@TestInstance(PER_CLASS)
class ScreenshotSurefireTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    File imageName;

    @BeforeAll
    void setup() {
        seleniumJupiter.getConfig().enableScreenshotAtTheEndOfTests();
        seleniumJupiter.getConfig().takeScreenshotAsPng();
        seleniumJupiter.getConfig().useSurefireOutputFolder();
    }

    @AfterAll
    void teardown() {
        assertTrue(imageName.exists());
        imageName.delete();
    }

    @Test
    void screenshotTest(ChromeDriver arg0) {
        arg0.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(arg0.getTitle()).contains("JUnit 5 extension for Selenium");

        imageName = new File(
                "./target/surefire-reports/io.github.bonigarcia.seljup.test.screenshot.ScreenshotSurefireTest",
                "screenshotTest_arg0_ChromeDriver_" + arg0.getSessionId()
                        + ".png");
    }

}
