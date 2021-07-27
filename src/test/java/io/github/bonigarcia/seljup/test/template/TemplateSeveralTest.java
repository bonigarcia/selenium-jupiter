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
package io.github.bonigarcia.seljup.test.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import io.github.bonigarcia.seljup.BrowserBuilder;
import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

class TemplateSeveralTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    @BeforeAll
    static void setup() {
        Browser chrome = BrowserBuilder.chrome().build();
        Browser firefox = BrowserBuilder.firefox().build();
        seleniumJupiter.addBrowsers(chrome);
        seleniumJupiter.addBrowsers(firefox);
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,
                "/dev/null");
    }

    @TestTemplate
    void templateTest1(WebDriver driver) {
        exercise(driver);
    }

    @TestTemplate
    void templateTest2(WebDriver driver) {
        exercise(driver);
    }

    private void exercise(WebDriver driver) {
        driver.get("https://bonigarcia.org/selenium-jupiter/");
        assertThat(driver.getTitle())
                .contains("JUnit 5 extension for Selenium");
    }

}
