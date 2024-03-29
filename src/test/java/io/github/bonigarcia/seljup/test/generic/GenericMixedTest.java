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
package io.github.bonigarcia.seljup.test.generic;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.seljup.SeleniumJupiter;

class GenericMixedTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    @BeforeAll
    static void setupClass() {
        seleniumJupiter.getConfig().enableScreenshotWhenFailure();
        seleniumJupiter.getConfig().takeScreenshotAsBase64();
    }

    @BeforeEach
    void setup() {
        System.setProperty("wdm.defaultBrowser", "chrome-in-docker");
    }

    @AfterEach
    void teardown() {
        System.clearProperty("wdm.defaultBrowser");
    }

    @Test
    void genericMixedTest(ChromeDriver local, RemoteWebDriver remote) {
        exercise(local);
        exercise(remote);
    }

    private void exercise(WebDriver driver) {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        driver.navigate().refresh();
        Wait<WebDriver> wait = new WebDriverWait(driver,
                Duration.ofSeconds(30));
        wait.until(d -> d.getTitle().contains("Selenium WebDriver"));
    }

}
