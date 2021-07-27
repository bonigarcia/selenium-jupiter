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
package io.github.bonigarcia.seljup.test.docker;

import static io.github.bonigarcia.seljup.BrowserType.CHROME_MOBILE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
@Disabled("Hardware server or virtual machine with nested virtualization support is required to run Chrome Mobile images")
class DockerChromeMobileVncJupiterTest {

    @Test
    void test(@DockerBrowser(type = CHROME_MOBILE, vnc = true) WebDriver driver)
            throws InterruptedException {
        driver.get("https://bonigarcia.org/selenium-jupiter/");
        assertThat(driver.getTitle())
                .contains("JUnit 5 extension for Selenium");

        // Uncomment this line to have more time for manually inspection
        // Thread.sleep(30000);
    }

}
