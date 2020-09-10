/*
 * (C) Copyright 2017 Boni Garcia (http://bonigarcia.github.io/)
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

// tag::snippet-in-doc[]
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openqa.selenium.remote.DesiredCapabilities.firefox;

// end::snippet-in-doc[]
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
// tag::snippet-in-doc[]
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// end::snippet-in-doc[]
import org.openqa.grid.selenium.GridLauncherV3;
// tag::snippet-in-doc[]
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.seljup.DriverCapabilities;
import io.github.bonigarcia.seljup.DriverUrl;
import io.github.bonigarcia.seljup.SeleniumJupiter;
// end::snippet-in-doc[]
import io.github.bonigarcia.wdm.WebDriverManager;

@Disabled
// tag::snippet-in-doc[]
@ExtendWith(SeleniumJupiter.class)
class RemoteWebDriverJupiterTest {

    @DriverUrl
    String url = "http://localhost:4444/wd/hub";

    @DriverCapabilities
    Capabilities capabilities = firefox();

    // end::snippet-in-doc[]
    @BeforeAll
    static void setup() throws Exception {
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
    }

    // tag::snippet-in-doc[]
    @Test
    void testWithRemoteChrome(@DriverUrl("http://localhost:4444/wd/hub")
            @DriverCapabilities("browserName=chrome") RemoteWebDriver driver) {
        exercise(driver);
    }

    @Test
    void testWithRemoteFirefox(RemoteWebDriver driver) {
        exercise(driver);
    }

    void exercise(WebDriver driver) {
        driver.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.getTitle(),
                containsString("JUnit 5 extension for Selenium"));
    }

}
// end::snippet-in-doc[]
