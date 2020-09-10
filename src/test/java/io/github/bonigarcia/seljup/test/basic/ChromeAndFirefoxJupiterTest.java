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
package io.github.bonigarcia.seljup.test.basic;

import org.junit.jupiter.api.Disabled;
// tag::snippet-in-doc[]
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;

// end::snippet-in-doc[]
@Disabled("Redudant test, only needed for doc")
// tag::snippet-in-doc[]
@ExtendWith(SeleniumJupiter.class)
class ChromeAndFirefoxJupiterTest {

    @Test
    void testWithOneChrome(ChromeDriver chromeDriver) {
        // Use Chrome in this test
    }

    @Test
    void testWithFirefox(FirefoxDriver firefoxDriver) {
        // Use Firefox in this test
    }

    @Test
    void testWithChromeAndFirefox(ChromeDriver chromeDriver,
            FirefoxDriver firefoxDriver) {
        // Use Chrome and Firefox in this test
    }

}
// end::snippet-in-doc[]
