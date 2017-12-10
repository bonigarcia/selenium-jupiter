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
package io.github.bonigarcia.test.forced;

import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumDriver;
import io.github.bonigarcia.Capability;
import io.github.bonigarcia.DriverCapabilities;
import io.github.bonigarcia.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class ForcedAppiumJupiterTest {

    static {
        setProperty("sel.jup.exception.when.no.driver", "false");
    }

    @Test
    void appiumNoCapabilitiesTest(AppiumDriver<WebElement> driver) {
        assertThat(driver, nullValue());
    }

    @Test
    void appiumWithCapabilitiesTest(@DriverCapabilities(capability = {
            @Capability(name = "browserName", value = "chrome"),
            @Capability(name = "deviceName", value = "Android") }) AppiumDriver<WebElement> driver) {
        assertThat(driver, nullValue());
    }

}
