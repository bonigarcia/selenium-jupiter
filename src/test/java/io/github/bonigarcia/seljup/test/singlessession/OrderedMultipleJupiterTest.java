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

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.SeleniumJupiter;
import io.github.bonigarcia.seljup.SingleSession;

@ExtendWith(SeleniumJupiter.class)
@TestMethodOrder(OrderAnnotation.class)
@SingleSession
class OrderedMultipleJupiterTest {

    final Logger log = getLogger(lookup().lookupClass());

    RemoteWebDriver driver1, driver2;

    OrderedMultipleJupiterTest(ChromeDriver driver1, RemoteWebDriver driver2) {
        this.driver1 = driver1;
        this.driver2 = driver2;
    }

    @Test
    @Order(1)
    void testStep1() {
        log.debug("Step 1: {} {}", driver1, driver2);
        step1(driver1);
        step1(driver2);
    }

    @Test
    @Order(2)
    void testStep2() {
        log.debug("Step 2: {} {}", driver1, driver2);
        step2(driver1);
        step2(driver2);
    }

    private void step1(RemoteWebDriver driver) {
        driver.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.getTitle())
                .contains("JUnit 5 extension for Selenium");
    }

    private void step2(RemoteWebDriver driver) {
        WebElement about = driver.findElementByLinkText("About");
        about.click();
        assertTrue(about.isDisplayed());
    }

}
