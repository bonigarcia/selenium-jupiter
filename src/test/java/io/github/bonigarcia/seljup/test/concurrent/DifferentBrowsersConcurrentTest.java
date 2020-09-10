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
package io.github.bonigarcia.seljup.test.concurrent;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
@Execution(CONCURRENT)
class DifferentBrowsersConcurrentTest {

    final Logger log = getLogger(lookup().lookupClass());

    @BeforeAll
    static void setup() {
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,
                "/dev/null");
    }

    @Test
    void testWithChrome1(ChromeDriver driver) {
        log.debug("#1 Chrome {}", driver);
        assertThat(driver, notNullValue());
        exercise(driver);
    }

    @Test
    void testWithFirefox1(FirefoxDriver driver) {
        log.debug("#1 Firefox {}", driver);
        assertThat(driver, notNullValue());
        exercise(driver);
    }

    @Test
    void testWithChrome2(ChromeDriver driver) {
        log.debug("#2 Chrome {}", driver);
        assertThat(driver, notNullValue());
        exercise(driver);
    }

    @Test
    void testWithFirefox2(FirefoxDriver driver) {
        log.debug("#2 Firefox {}", driver);
        assertThat(driver, notNullValue());
        exercise(driver);
    }

    void exercise(WebDriver driver) {
        driver.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.getTitle(),
                containsString("JUnit 5 extension for Selenium"));
    }

}
