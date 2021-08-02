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
package io.github.bonigarcia.seljup.test.docker;

import static io.github.bonigarcia.seljup.BrowserType.CHROME;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
class DockerMixedListInConstructorTest {

    static final int NUM_BROWSERS = 1;

    WebDriver driver1;
    List<WebDriver> driverList1;

    DockerMixedListInConstructorTest(RemoteWebDriver driver1,
            @DockerBrowser(type = CHROME, size = NUM_BROWSERS) List<WebDriver> driverList1) {
        this.driver1 = driver1;
        this.driverList1 = driverList1;
    }

    @Test
    void testGlobalChrome(WebDriver driver2,
            @DockerBrowser(type = CHROME, size = NUM_BROWSERS) List<RemoteWebDriver> driverList2) {
        exercise(driver1);
        driverList1.forEach(this::exercise);
        exercise(driver2);
        driverList2.forEach(this::exercise);
    }

    private void exercise(WebDriver driver) {
        driver.get("https://bonigarcia.org/selenium-jupiter/");

        Wait<WebDriver> wait = new WebDriverWait(driver,
                Duration.ofSeconds(30));
        wait.until(d -> d.getTitle().contains("Selenium-Jupiter"));
    }

}
