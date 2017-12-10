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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;

import io.github.bonigarcia.DriverOptions;
import io.github.bonigarcia.SeleniumExtension;

@Disabled("Opera not available on Travis CI")
@ExtendWith(SeleniumExtension.class)
public class OperaWithGlobalOptionsJupiterTest {

    @DriverOptions
    OperaOptions operaOptions = new OperaOptions();
    {
        operaOptions
                .setBinary(new File("C:\\Program Files\\Opera\\launcher.exe"));
    }

    @Test
    public void operaTest(OperaDriver driver) {
        driver.get("http://www.seleniumhq.org/");
        assertThat(driver.getTitle(),
                containsString("A JUnit 5 extension for Selenium WebDriver"));
    }

}
// end::snippet-in-doc[]
