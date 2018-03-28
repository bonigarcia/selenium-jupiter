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
package io.github.bonigarcia.test.docker;

import static io.github.bonigarcia.BrowserType.CHROME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumExtension;
import io.github.bonigarcia.SeleniumJupiter;

@ExtendWith(SeleniumExtension.class)
@TestInstance(PER_CLASS)
public class DockerHtmlVncJupiterTest {

    File htmlFile;

    @BeforeAll
    void setup() {
        SeleniumJupiter.config().setVnc(true);
        SeleniumJupiter.config().setVncRedirectHtmlPage(true);
        SeleniumJupiter.config().useSurefireOutputFolder();
    }

    @AfterAll
    void teardown() {
        SeleniumJupiter.config().reset();
        assertTrue(htmlFile.exists());
        htmlFile.delete();
    }

    @Test
    public void testHtmlVnc(
            @DockerBrowser(type = CHROME, version = "64.0") RemoteWebDriver driver) {
        driver.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.getTitle(),
                containsString("A JUnit 5 extension for Selenium WebDriver"));

        String folder = "target/surefire-reports/io.github.bonigarcia.test.docker.DockerHtmlVncJupiterTest";
        htmlFile = new File(folder, "testHtmlVnc_arg0_CHROME_64.0_"
                + driver.getSessionId() + ".html");
    }

}
