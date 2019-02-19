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
package io.github.bonigarcia.seljup.test.appium;

// tag::snippet-in-doc[]
import static org.junit.jupiter.api.Assertions.assertTrue;

// end::snippet-in-doc[]
import org.junit.jupiter.api.Disabled;
// tag::snippet-in-doc[]
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumDriver;
import io.github.bonigarcia.seljup.DriverCapabilities;
import io.github.bonigarcia.seljup.SeleniumExtension;

// end::snippet-in-doc[]
@Disabled("Android emulator not available on Travis CI")
// tag::snippet-in-doc[]
@ExtendWith(SeleniumExtension.class)
public class AppiumChromeJupiterTest {

    @Test
    void testWithAndroid(
            @DriverCapabilities({ "browserName=chrome",
                    "deviceName=Android" }) AppiumDriver<WebElement> driver)
            throws InterruptedException {

        String context = driver.getContext();
        driver.context("NATIVE_APP");
        driver.findElement(By.id("com.android.chrome:id/terms_accept")).click();
        driver.findElement(By.id("com.android.chrome:id/negative_button"))
                .click();
        driver.context(context);

        driver.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertTrue(driver.getTitle().contains("JUnit 5 extension"));
    }

}
// end::snippet-in-doc[]