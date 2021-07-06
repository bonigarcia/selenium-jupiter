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
package io.github.bonigarcia.seljup.test.advance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
// tag::snippet-in-doc[]
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;
import io.github.bonigarcia.wdm.WebDriverManager;

@ExtendWith(SeleniumJupiter.class)
@Disabled("Redundant test")
class MultipleConfigWdmJupiterTest {

    @BeforeAll
    static void setup() {
        WebDriverManager.chromedriver().forceDownload();
        WebDriverManager.firefoxdriver().useLocalVersionsPropertiesFirst();
    }

    @Test
    void multipleConfigTest(ChromeDriver chrome, FirefoxDriver firefox) {
        String sut = "https://bonigarcia.github.io/selenium-jupiter/";
        String title = "JUnit 5 extension for Selenium";

        chrome.get(sut);
        firefox.get(sut);

        assertThat(chrome.getTitle()).contains(title);
        assertThat(firefox.getTitle()).contains(title);
    }

}
