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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.github.bonigarcia.seljup.DriverCapabilities;
import io.github.bonigarcia.seljup.SeleniumExtension;
import java.io.File;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

// end::snippet-in-doc[]
@Disabled("Android emulator not available on Travis CI")
// tag::snippet-in-doc[]
public class AppiumApkLogLevelJupiterTest {

    @RegisterExtension
    static SeleniumExtension seleniumExtension = new SeleniumExtension();

    /**
     * Configure Selenium to use a custom log level
     */
    @BeforeEach
    void setupSelenium() {
        seleniumExtension.getConfig().setAndroidAppiumLogLevel("error:debug");
        seleniumExtension.getConfig().setAndroidAppiumLogFile("appiumLog");
    }

    @DriverCapabilities
    DesiredCapabilities capabilities = new DesiredCapabilities();
    {
        try {
            File apk = new File(this.getClass()
                    .getResource("/selendroid-test-app.apk").toURI());
            capabilities.setCapability("app", apk.getAbsolutePath());
            capabilities.setCapability("deviceName", "Android");

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testWithAndroid(AppiumDriver<MobileElement> driver)
            throws InterruptedException {
        WebElement button = driver.findElement(By.id("buttonStartWebview"));
        assertThat(button, notNullValue());
        button.click();

        WebElement inputField = driver.findElement(By.id("name_input"));
        inputField.clear();
        inputField.sendKeys("Custom name");
    }

}
// end::snippet-in-doc[]
