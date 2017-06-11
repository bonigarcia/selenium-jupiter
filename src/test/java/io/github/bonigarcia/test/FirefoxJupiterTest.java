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
package io.github.bonigarcia.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.firefox.FirefoxDriver;

import io.github.bonigarcia.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class FirefoxJupiterTest {

    @Test
    public void test1(FirefoxDriver firefox) {
        firefox.get("http://www.seleniumhq.org/");
        String title = firefox.getTitle();

        assertTrue(title.equals("Selenium - Web Browser Automation"));
    }

    @Test
    public void test2(FirefoxDriver firefox1, FirefoxDriver firefox2) {
        firefox1.get("http://www.seleniumhq.org/");
        String title1 = firefox1.getTitle();
        firefox2.get("http://junit.org/junit5/");
        String title2 = firefox2.getTitle();

        assertTrue(title1.equals("Selenium - Web Browser Automation"));
        assertTrue(title2.equals("JUnit 5"));
    }

}
