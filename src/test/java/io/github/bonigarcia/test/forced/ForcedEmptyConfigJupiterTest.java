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
package io.github.bonigarcia.test.forced;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.SeleniumExtension;
import io.github.bonigarcia.SeleniumJupiter;
import io.github.bonigarcia.config.Config;

@ExtendWith(SeleniumExtension.class)
public class ForcedEmptyConfigJupiterTest {

    @BeforeAll
    static void setup() throws Exception {
        Config config = SeleniumJupiter.config();
        config.setExceptionWhenNoDriver(false);
        for (Method method : config.getClass().getMethods()) {
            if (method.getName().startsWith("set")) {
                if (method.getParameterTypes()[0].equals(String.class)) {
                    method.invoke(config, "");
                }
            }
        }
    }

    @AfterAll
    static void teardown() {
        SeleniumJupiter.config().reset();
    }

    @Test
    void genericTest(WebDriver driver) {
        assertThrows(NullPointerException.class, () -> driver
                .get("https://bonigarcia.github.io/selenium-jupiter/"));
    }

}
