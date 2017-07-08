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
package io.github.bonigarcia.test.basic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.safari.SafariDriver;

import io.github.bonigarcia.SeleniumExtension;

/**
 * Test with Chrome browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
@ExtendWith(SeleniumExtension.class)
public class OtherJupiterTest {

    @Disabled("WebDriverManager 1.7.1 required")
    @Test
    public void htmlUnitTest(HtmlUnitDriver htmlUnitDriver) {
        htmlUnitDriver.get("http://www.seleniumhq.org/");
        String title = htmlUnitDriver.getTitle();

        assertTrue(title.equals("Selenium - Web Browser Automation"));
    }

    @Disabled("SafariDriver requires Safari 10 running on OSX El Capitan or greater.")
    @Test
    public void safariTest(SafariDriver safari) {
        assertThrows(ParameterResolutionException.class, () -> {
            safari.get("http://www.seleniumhq.org/");
        });
    }

}
