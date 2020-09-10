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
package io.github.bonigarcia.seljup.test.template;

// tag::snippet-in-doc[]
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeAll;
// tag::snippet-in-doc[]
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;

class TemplateTwoBrowsersTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    // end::snippet-in-doc[]
    @BeforeAll
    static void setup() {
        seleniumJupiter.getConfig().setBrowserTemplateJsonFile(
                "./src/test/resources/browsers-two.json");
    }

    // tag::snippet-in-doc[]
    @TestTemplate
    void templateTest(WebDriver driver1, WebDriver driver2) {
        driver1.get("https://bonigarcia.github.io/selenium-jupiter/");
        driver2.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver1.getTitle(),
                containsString("JUnit 5 extension for Selenium"));
        assertThat(driver2.getTitle(),
                containsString("JUnit 5 extension for Selenium"));
    }

}
// end::snippet-in-doc[]