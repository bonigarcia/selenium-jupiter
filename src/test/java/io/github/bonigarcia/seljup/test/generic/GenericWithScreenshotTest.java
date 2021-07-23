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
package io.github.bonigarcia.seljup.test.generic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;

@TestInstance(PER_CLASS)
@Disabled("Disable temporary")
class GenericWithScreenshotTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    File imageFile;

    @BeforeAll
    void setup() {
        seleniumJupiter.getConfig().enableScreenshotAtTheEndOfTests();
        seleniumJupiter.getConfig().takeScreenshotAsPng();
        // TODO seleniumJupiter.getConfig().setDefaultVersion("91.0");
    }

    @AfterAll
    void teardown() {
        assertTrue(imageFile.exists());
        imageFile.delete();
    }

    @Test
    void screenshotGenericTest(WebDriver arg0) {
        arg0.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(arg0.getTitle()).contains("JUnit 5 extension for Selenium");

        imageFile = new File("screenshotGenericTest_arg0_CHROME_91.0_"
                + ((RemoteWebDriver) arg0).getSessionId() + ".png");
    }

}
