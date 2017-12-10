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
package io.github.bonigarcia.test.forced;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.safari.SafariDriver;

import io.github.bonigarcia.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class ForcedSafariJupiterTest {

    @BeforeEach
    void setup() {
        setProperty("sel.jup.exception.when.no.driver", "false");
    }

    @Test
    void safariTest(SafariDriver driver) {
        assumeFalse(IS_OS_MAC);
        assertThat(driver, nullValue());
    }

    @BeforeEach
    void teardown() {
        clearProperty("sel.jup.exception.when.no.driver");
    }

}
