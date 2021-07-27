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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxDriver;

import io.github.bonigarcia.seljup.Arguments;
import io.github.bonigarcia.seljup.Extensions;
import io.github.bonigarcia.seljup.Preferences;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
class FirefoxWithOptionsJupiterTest {

    @Test
    void webrtcTest(@Arguments("-private") @Preferences({
            "media.navigator.permission.disabled=true",
            "media.navigator.streams.fake=true" }) FirefoxDriver driver) {
        driver.get(
                "https://webrtc.github.io/samples/src/content/devices/input-output/");
        assertThat(driver.findElement(By.id("video")).getTagName())
                .isEqualTo("video");
    }

    @Test
    void extensionTest(@Extensions("hello_world.xpi") FirefoxDriver driver) {
        driver.get("https://bonigarcia.org/selenium-jupiter/");
        assertThat(driver.getTitle())
                .contains("JUnit 5 extension for Selenium");
    }

}
// end::snippet-in-doc[]
