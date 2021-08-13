/*
 * (C) Copyright 2019 Boni Garcia (https://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup.test.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import io.github.bonigarcia.seljup.Options;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
class MixedOptionsTest {

    @Options
    ChromeOptions chromeOptions = new ChromeOptions();
    {
        chromeOptions.addArguments("--use-fake-device-for-media-stream",
                "--use-fake-ui-for-media-stream");
    }

    @Options
    FirefoxOptions firefoxOptions = new FirefoxOptions();
    {
        firefoxOptions.addPreference("media.navigator.streams.fake", true);
        firefoxOptions.addPreference("media.navigator.permission.disabled",
                true);
    }

    @Test
    void webrtcTest(ChromeDriver chrome, FirefoxDriver firefox) {
        exercise(chrome, firefox);
    }

    void exercise(WebDriver... drivers) {
        for (WebDriver driver : drivers) {
            driver.get(
                    "https://webrtc.github.io/samples/src/content/devices/input-output/");
            assertThat(driver.findElement(By.id("video")).getTagName())
                    .isEqualTo("video");
        }
    }

}
