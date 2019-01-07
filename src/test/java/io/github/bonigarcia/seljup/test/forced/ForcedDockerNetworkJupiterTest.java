/*
 * (C) Copyright 2018 Boni Garcia (http://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup.test.forced;

import static io.github.bonigarcia.seljup.BrowserType.CHROME;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumExtension;

public class ForcedDockerNetworkJupiterTest {

    @RegisterExtension
    static SeleniumExtension seleniumExtension = new SeleniumExtension();

    @BeforeEach
    void setup() {
        seleniumExtension.getConfig().setExceptionWhenNoDriver(false);
        seleniumExtension.getConfig().setDockerNetwork("host");
    }

    @Test
    public void test(@DockerBrowser(type = CHROME) WebDriver driver) {
        assertThrows(NullPointerException.class, () -> driver
                .get("https://bonigarcia.github.io/selenium-jupiter/"));
    }

}
