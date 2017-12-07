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
package io.github.bonigarcia.test.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.firefox.FirefoxOptions.FIREFOX_OPTIONS;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import io.github.bonigarcia.handler.FirefoxDriverHandler;
import io.github.bonigarcia.test.advance.FirefoxWithOptionsJupiterTest;
import io.github.bonigarcia.test.mockito.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FirefoxAnnotationReaderTest {

    @InjectMocks
    FirefoxDriverHandler annotationsReader;

    @Test
    @SuppressWarnings("unchecked")
    void testFirefoxOptions() throws Exception {
        Parameter parameter = FirefoxWithOptionsJupiterTest.class
                .getMethod("webrtcFirefoxTest", FirefoxDriver.class)
                .getParameters()[0];
        FirefoxOptions firefoxOptions = annotationsReader
                .getFirefoxOptions(parameter, Optional.empty());
        Map<String, Map<String, Boolean>> options = (Map<String, Map<String, Boolean>>) firefoxOptions
                .asMap().get(FIREFOX_OPTIONS);

        assertTrue(options.get("prefs")
                .get("media.navigator.permission.disabled"));
        assertTrue(options.get("prefs").get("media.navigator.streams.fake"));
    }

}
