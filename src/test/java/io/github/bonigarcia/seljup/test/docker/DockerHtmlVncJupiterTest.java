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
package io.github.bonigarcia.seljup.test.docker;

import static io.github.bonigarcia.seljup.BrowserType.CHROME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumExtension;

@TestInstance(PER_CLASS)
public class DockerHtmlVncJupiterTest {

    @RegisterExtension
    static SeleniumExtension seleniumExtension = new SeleniumExtension();

    File htmlFile;

    @BeforeAll
    void setup() {
        seleniumExtension.getConfig().setVnc(true);
        seleniumExtension.getConfig().setVncRedirectHtmlPage(true);
        seleniumExtension.getConfig().useSurefireOutputFolder();
    }

    @AfterAll
    void teardown() {
        assertTrue(htmlFile.exists());
        htmlFile.delete();
    }

    @Test
    public void testHtmlVnc(
            @DockerBrowser(type = CHROME, version = "73.0") RemoteWebDriver arg0) {
        arg0.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(arg0.getTitle(),
                containsString("JUnit 5 extension for Selenium"));

        String folder = "target/surefire-reports/io.github.bonigarcia.seljup.test.docker.DockerHtmlVncJupiterTest";
        htmlFile = new File(folder, "testHtmlVnc_arg0_CHROME_73.0_"
                + arg0.getSessionId() + ".html");
    }

}
