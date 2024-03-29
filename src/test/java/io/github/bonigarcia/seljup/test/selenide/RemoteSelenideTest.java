/*
 * (C) Copyright 2017 Boni Garcia (https://bonigarcia.github.io/)
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;

import com.codeborne.selenide.SelenideDriver;

import io.github.bonigarcia.seljup.DriverCapabilities;
import io.github.bonigarcia.seljup.EnabledIfDriverUrlOnline;
import io.github.bonigarcia.seljup.SeleniumJupiter;

//tag::snippet-in-doc[]
@EnabledIfDriverUrlOnline("http://localhost:4444/")
@ExtendWith(SeleniumJupiter.class)
class RemoteSelenideTest {

    @DriverCapabilities
    Capabilities capabilities = new ChromeOptions();

    @Test
    void test(SelenideDriver driver) {
        driver.open("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.title()).contains("Selenium WebDriver");
    }

}
//end::snippet-in-doc[]