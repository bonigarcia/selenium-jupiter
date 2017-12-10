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
package io.github.bonigarcia.test.advance;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

// tag::snippet-in-doc[]
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import io.github.bonigarcia.DriverOptions;
import io.github.bonigarcia.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class FirefoxWithGlobalOptionsJupiterTest {

    @DriverOptions
    FirefoxOptions firefoxOptions = new FirefoxOptions();
    {
        // Flag to use fake media for WebRTC user media
        firefoxOptions.addPreference("media.navigator.streams.fake", true);

        // Flag to avoid granting access to user media
        firefoxOptions.addPreference("media.navigator.permission.disabled",
                true);
    }

    @Test
    public void webrtcTest(FirefoxDriver driver) {
        driver.get(
                "https://webrtc.github.io/samples/src/content/devices/input-output/");
        assertThat(driver.findElement(By.id("video")).getTagName(),
                equalTo("video"));
    }

}
// end::snippet-in-doc[]
