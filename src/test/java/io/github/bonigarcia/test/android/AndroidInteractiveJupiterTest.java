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
package io.github.bonigarcia.test.android;

import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import io.github.bonigarcia.SeleniumJupiter;

/**
 * Test interactive mode for Android (from the shell).
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.1.0
 */
class AndroidInteractiveJupiterTest {

    @Test
    void testInteractiveAndroid() {
        exercise(new String[] { "android", "8.0", "Samsung Galaxy S6" });
    }

    void exercise(String[] args) {
        assertTimeout(ofMinutes(5), () -> {
            ByteArrayInputStream intro = new ByteArrayInputStream(
                    "\r\n".getBytes());
            System.setIn(intro);
            SeleniumJupiter.main(args);
        });
    }

}
