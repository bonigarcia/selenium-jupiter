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
import static io.github.bonigarcia.SeleniumJupiter.BINARY;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.edge.EdgeDriver;

import io.github.bonigarcia.DriverOptions;
import io.github.bonigarcia.Option;
import io.github.bonigarcia.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class OperaWithOptionsJupiterTest {

    @Disabled("Opera not available on Travis CI")
    @Test
    public void operaTest(@DriverOptions(options = {
            @Option(name = BINARY, value = "C:\\Program Files\\Opera\\launcher.exe") }) EdgeDriver edge) {
        edge.get("http://www.seleniumhq.org/");

        assertTrue(edge.getTitle().equals("Selenium - Web Browser Automation"));
    }

}
// end::snippet-in-doc[]
