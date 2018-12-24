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
package io.github.bonigarcia.test.template;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.grid.selenium.GridLauncherV3;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.BrowserBuilder;
import io.github.bonigarcia.BrowsersTemplate.Browser;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.SeleniumExtension;

public class TemplateRemoteTest {

    @RegisterExtension
    static SeleniumExtension seleniumExtension = new SeleniumExtension();

    @BeforeAll
    static void setup() {
        // Start hub
        GridLauncherV3.main(new String[] { "-role", "hub", "-port", "4444" });

        // Register Chrome in hub
        WebDriverManager.chromedriver().setup();
        GridLauncherV3.main(new String[] { "-role", "node", "-hub",
                "http://localhost:4444/grid/register", "-browser",
                "browserName=chrome", "-port", "5555" });

        // Register Firefox in hub
        WebDriverManager.firefoxdriver().setup();
        GridLauncherV3.main(new String[] { "-role", "node", "-hub",
                "http://localhost:4444/grid/register", "-browser",
                "browserName=firefox", "-port", "5556" });

        // Register Chrome and Firefox in template
        Browser chrome = BrowserBuilder.chrome()
                .url("http://localhost:4444/wd/hub").build();
        Browser firefox = BrowserBuilder.firefox()
                .url("http://localhost:4444/wd/hub").build();
        seleniumExtension.addBrowsers(chrome);
        seleniumExtension.addBrowsers(firefox);
    }

    @TestTemplate
    void templateTest(WebDriver driver) {
        driver.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.getTitle(),
                containsString("JUnit 5 extension for Selenium"));
    }

}
