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
package io.github.bonigarcia.seljup.test.capabilities;

import static io.github.bonigarcia.seljup.Browser.SAFARI;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import io.github.bonigarcia.seljup.EnabledIfBrowserAvailable;
import io.github.bonigarcia.seljup.Options;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@EnabledIfBrowserAvailable(SAFARI)
@ExtendWith(SeleniumJupiter.class)
class SafariWithGlobalOptionsTest {

    @Options
    SafariOptions safariOptions = new SafariOptions();
    {
        safariOptions.setUseTechnologyPreview(false);
    }

    @Test
    void safariTest(SafariDriver driver) {
        driver.get("https://bonigarcia.org/selenium-jupiter/");
        assertThat(driver.getTitle()).contains("Selenium-Jupiter");
    }

}