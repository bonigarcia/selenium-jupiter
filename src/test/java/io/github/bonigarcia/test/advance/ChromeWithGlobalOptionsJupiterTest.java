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

// tag::snippet-in-doc[]
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.DriverOptions;
import io.github.bonigarcia.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class ChromeWithGlobalOptionsJupiterTest {

    @DriverOptions
    ChromeOptions chromeOptions = new ChromeOptions();
    {
        // Flags to use fake media for WebRTC user media
        chromeOptions.addArguments("--use-fake-device-for-media-stream",
                "--use-fake-ui-for-media-stream");
    }

    @Test
    void webrtcTest1(ChromeDriver chrome) {
        chrome.get(
                "https://webrtc.github.io/samples/src/content/devices/input-output/");
    }

    @Test
    void webrtcTest2(ChromeDriver chrome) {
        chrome.get(
                "https://webrtc.github.io/samples/src/content/getusermedia/gum/");
    }

}
// end::snippet-in-doc[]
