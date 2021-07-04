/*
 * (C) Copyright 2019 Boni Garcia (http://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup.test.singlessession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;
import io.github.bonigarcia.seljup.SingleSession;

@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@SingleSession
@Disabled
class FailureCreatesScreenshotTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    WebDriver driver;

    FailureCreatesScreenshotTest(WebDriver driver) {
        this.driver = driver;
    }

    @BeforeAll
    void setup() {
        seleniumJupiter.getConfig().setScreenshotAtTheEndOfTests("whenfailure");
        seleniumJupiter.getConfig().takeScreenshotAsPng();
    }

    @Test
    void shouldFailAndCreateScreenshotTest() {
        driver.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.getTitle())
                .contains("JUnit 5 extension for Selenium");
    }

    @Test
    void notFailAndNotCreateScreenshotTest() {
        driver.get("https://bonigarcia.github.io/");
        assertThat(driver.getTitle()).contains("Boni");
    }

}
