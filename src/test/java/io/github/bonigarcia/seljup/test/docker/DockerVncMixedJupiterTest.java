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
import static io.github.bonigarcia.seljup.BrowserType.FIREFOX;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

class DockerVncMixedJupiterTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    @Test
    void testHtmlVnc(@DockerBrowser(type = CHROME) WebDriver driver1,
            @DockerBrowser(type = FIREFOX) WebDriver driver2) {
        driver1.get("https://bonigarcia.org/selenium-jupiter/");
        driver2.get("https://bonigarcia.org/selenium-jupiter/");

        assertThat(driver1.getTitle()).contains("Selenium-Jupiter");
        assertThat(driver2.getTitle()).contains("Selenium-Jupiter");
    }

}
