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

// end::snippet-in-doc[]
import org.junit.jupiter.api.Disabled;
// tag::snippet-in-doc[]
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumExtension;

// end::snippet-in-doc[]
@Disabled("Redudant test, only needed for doc")
// tag::snippet-in-doc[]
@ExtendWith(SeleniumExtension.class)
class DockerChromeLatestJupiterTest {

    @Test
    void testLatestChrome(
            @DockerBrowser(type = CHROME, version = "latest") WebDriver driver) {
        // Use stable version of Chrome in this test
    }

    @Test
    void testFormerChrome(
            @DockerBrowser(type = CHROME, version = "latest-1") WebDriver driver) {
        // Use previous to stable version of Chrome in this test
    }

    @Test
    void testBetaChrome(
            @DockerBrowser(type = CHROME, version = "beta") WebDriver driver) {
        // Use beta version of Chrome in this test
    }

    @Test
    void testUnstableChrome(
            @DockerBrowser(type = CHROME, version = "unstable") WebDriver driver) {
        // Use development version of Chrome in this test
    }

}
// end::snippet-in-doc[]
