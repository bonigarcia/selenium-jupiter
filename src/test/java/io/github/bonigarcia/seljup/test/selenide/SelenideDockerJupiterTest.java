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
import static io.github.bonigarcia.seljup.BrowserType.CHROME;
import static io.github.bonigarcia.seljup.BrowserType.FIREFOX;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.codeborne.selenide.SelenideDriver;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumExtension;

public class SelenideDockerJupiterTest {

    @RegisterExtension
    static SeleniumExtension seleniumExtension = new SeleniumExtension();

    @BeforeAll
    static void setup() {
        seleniumExtension.getConfig().setVnc(true);
    }

    @Test
    public void testDockerSelenideChrome(
            @DockerBrowser(type = CHROME) SelenideDriver driver) {
        driver.open("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.title(),
                containsString("JUnit 5 extension for Selenium"));
    }

    @Test
    public void testDockerSelenideFirefox(
            @DockerBrowser(type = FIREFOX) SelenideDriver driver) {
        driver.open("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.title(),
                containsString("JUnit 5 extension for Selenium"));
    }

}
// end::snippet-in-doc[]
