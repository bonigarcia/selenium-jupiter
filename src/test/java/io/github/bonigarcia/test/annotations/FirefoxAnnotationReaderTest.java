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
package io.github.bonigarcia.test.annotations;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.firefox.FirefoxOptions.FIREFOX_OPTIONS;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import io.github.bonigarcia.handler.FirefoxDriverHandler;
import io.github.bonigarcia.test.advance.FirefoxWithGlobalOptionsJupiterTest;
import io.github.bonigarcia.test.advance.FirefoxWithOptionsJupiterTest;

@ExtendWith(MockitoExtension.class)
public class FirefoxAnnotationReaderTest {

    @InjectMocks
    FirefoxDriverHandler annotationsReader;

    static Stream<Class<?>> testClassProvider() {
        return Stream.of(FirefoxWithOptionsJupiterTest.class,
                FirefoxWithGlobalOptionsJupiterTest.class);
    }

    @ParameterizedTest
    @MethodSource("testClassProvider")
    @SuppressWarnings("unchecked")
    void testFirefoxOptions(Class<?> testClass) throws Exception {
        Parameter parameter = testClass
                .getMethod("webrtcTest", FirefoxDriver.class)
                .getParameters()[0];
        Optional<Object> testInstance = Optional.of(testClass.newInstance());

        FirefoxOptions firefoxOptions = (FirefoxOptions) annotationsReader
                .getOptions(parameter, testInstance);
        Map<String, Map<String, Boolean>> options = (Map<String, Map<String, Boolean>>) firefoxOptions
                .asMap().get(FIREFOX_OPTIONS);

        assertTrue(options.get("prefs")
                .get("media.navigator.permission.disabled"));
        assertTrue(options.get("prefs").get("media.navigator.streams.fake"));
    }

    @Test
    void testAnnotatedFirefoxOptionsIsSelectedOverOtherAnnotatedOptions() throws Exception {
        Optional<Object> testInstance = Optional.of(new ClassWithMultipleOptions());
        FirefoxOptions firefoxOptions = (FirefoxOptions) annotationsReader.getOptions(null, testInstance);
        assertThat(firefoxOptions, notNullValue());
    }
}
