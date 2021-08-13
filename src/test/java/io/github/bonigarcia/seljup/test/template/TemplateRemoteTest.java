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
import static org.openqa.selenium.net.PortProber.findFreePort;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.grid.Main;

import io.github.bonigarcia.seljup.BrowserBuilder;
import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import io.github.bonigarcia.seljup.SeleniumJupiter;
import io.github.bonigarcia.wdm.WebDriverManager;

class TemplateRemoteTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    @BeforeAll
    static void setup() {
        // Resolve drivers
        WebDriverManager.chromedriver().setup();
        WebDriverManager.firefoxdriver().setup();

        // Start Selenium Grid in standalone mode
        int port = findFreePort();
        Main.main(
                new String[] { "standalone", "--port", String.valueOf(port) });

        // Register Chrome and Firefox in the browser scenario for the template
        String serverUrl = "http://localhost:" + port + "/wd/hub";
        Browser chrome = BrowserBuilder.chrome().remoteUrl(serverUrl).build();
        Browser firefox = BrowserBuilder.firefox().remoteUrl(serverUrl).build();
        seleniumJupiter.addBrowsers(chrome);
        seleniumJupiter.addBrowsers(firefox);
    }

    @TestTemplate
    void templateTest(WebDriver driver) {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.getTitle()).contains("Selenium WebDriver");
    }

}
