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
package io.github.bonigarcia.seljup.test.screenshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;

//tag::snippet-in-doc[]
class ScreenshotSurefireTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    static File imageFile;

    @BeforeAll
    static void setup() {
        seleniumJupiter.getConfig().enableScreenshot();
        seleniumJupiter.getConfig().takeScreenshotAsPng();
        seleniumJupiter.getConfig().useSurefireOutputFolder();
    }

    @AfterAll
    static void teardown() {
        assertThat(imageFile).exists();
        imageFile.delete();
    }

    @Test
    void screenshotTest(ChromeDriver driver) {
        driver.get("https://bonigarcia.org/selenium-jupiter/");
        assertThat(driver.getTitle()).contains("Selenium-Jupiter");

        imageFile = new File(
                "./target/surefire-reports/io.github.bonigarcia.seljup.test.screenshot.ScreenshotSurefireTest",
                "screenshotTest_" + driver.getSessionId() + ".png");
    }

}
//end::snippet-in-doc[]
