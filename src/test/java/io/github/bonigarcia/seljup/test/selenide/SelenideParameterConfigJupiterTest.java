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
package io.github.bonigarcia.seljup.test.selenide;

// tag::snippet-in-doc[]
import static com.codeborne.selenide.Browsers.CHROME;
import static com.codeborne.selenide.Browsers.FIREFOX;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.codeborne.selenide.SelenideDriver;

import io.github.bonigarcia.seljup.SelenideConfiguration;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
class SelenideParameterConfigJupiterTest {

    @Test
    void testHeadlessFirefoxSelenide(
            @SelenideConfiguration(browser = FIREFOX, headless = true) SelenideDriver driver) {
        exercise(driver);
    }

    @Test
    void testChromeAndFirefoxSelenide(
            @SelenideConfiguration(browser = CHROME) SelenideDriver chrome,
            @SelenideConfiguration(browser = FIREFOX) SelenideDriver firefox) {
        exercise(chrome, firefox);
    }

    private void exercise(SelenideDriver... drivers) {
        for (SelenideDriver driver : drivers) {
            driver.open("https://bonigarcia.dev/selenium-webdriver-java/");
            assertThat(driver.title()).contains("Selenium WebDriver");
        }
    }

}
// end::snippet-in-doc[]
