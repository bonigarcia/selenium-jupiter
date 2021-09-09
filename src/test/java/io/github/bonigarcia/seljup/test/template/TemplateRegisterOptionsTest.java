/*
 * (C) Copyright 2021 Boni Garcia (https://bonigarcia.github.io/)
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

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.seljup.BrowserBuilder;
import io.github.bonigarcia.seljup.Options;
import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

class TemplateRegisterOptionsTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    @Options
    static ChromeOptions chromeOptions = new ChromeOptions();

    @BeforeAll
    static void setup() {
        chromeOptions.addArguments("--use-fake-device-for-media-stream",
                "--use-fake-ui-for-media-stream");

        Browser chrome = BrowserBuilder.chrome().build();
        Browser chromeInDocker = BrowserBuilder.chromeInDocker().build();
        seleniumJupiter.addBrowsers(chrome);
        seleniumJupiter.addBrowsers(chromeInDocker);

        seleniumJupiter.getConfig().enableScreenshot();
    }

    @TestTemplate
    void templateTest(WebDriver driver) throws InterruptedException {
        driver.get(
                "https://webrtc.github.io/samples/src/content/devices/input-output/");
        assertThat(driver.findElement(By.id("video")).getTagName())
                .isEqualTo("video");

        Thread.sleep(Duration.ofSeconds(5).toMillis());
    }

}
