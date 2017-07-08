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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.SeleniumExtension;

/**
 * Test with Chrome browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
@ExtendWith(SeleniumExtension.class)
public class ChromeJupiterTest {

    @Test
    public void test1(ChromeDriver chrome) {
        chrome.get("http://www.seleniumhq.org/");
        String title = chrome.getTitle();

        assertTrue(title.equals("Selenium - Web Browser Automation"));
    }

    @Test
    public void test2(ChromeDriver chrome1, ChromeDriver chrome2) {
        chrome1.get("http://www.seleniumhq.org/");
        String title1 = chrome1.getTitle();
        chrome2.get("http://junit.org/junit5/");
        String title2 = chrome2.getTitle();

        assertTrue(title1.equals("Selenium - Web Browser Automation"));
        assertTrue(title2.equals("JUnit 5"));
    }

}
