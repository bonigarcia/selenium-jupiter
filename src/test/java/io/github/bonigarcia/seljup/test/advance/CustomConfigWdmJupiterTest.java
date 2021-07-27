/*
 * (C) Copyright 2021 Boni Garcia (http://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup.test.advance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
// tag::snippet-in-doc[]
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;
import io.github.bonigarcia.wdm.WebDriverManager;

class CustomConfigWdmJupiterTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    @BeforeAll
    static void setup() {
        WebDriverManager wdm = WebDriverManager.chromedriver().browserInDocker()
                .dockerImage("selenoid/vnc:chrome_91.0");
        seleniumJupiter.getConfig().setManager(wdm);
    }

    @Test
    void multipleConfigTest(WebDriver driver) {
        String sut = "https://bonigarcia.org/selenium-jupiter/";
        String title = "JUnit 5 extension for Selenium";

        driver.get(sut);
        assertThat(driver.getTitle()).contains(title);
    }

}
