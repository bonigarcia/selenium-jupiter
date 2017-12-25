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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import io.github.bonigarcia.handler.SafariDriverHandler;
import io.github.bonigarcia.test.advance.SafariWithGlobalOptionsJupiterTest;
import io.github.bonigarcia.test.advance.SafariWithOptionsJupiterTest;
import io.github.bonigarcia.test.mockito.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SafariAnnotationReaderTest {

    @InjectMocks
    SafariDriverHandler annotationsReader;

    static Stream<Class<?>> testClassProvider() {
        return Stream.of(SafariWithOptionsJupiterTest.class,
                SafariWithGlobalOptionsJupiterTest.class);
    }

    @ParameterizedTest
    @MethodSource("testClassProvider")
    @SuppressWarnings("deprecation")
    void testSafariOptions(Class<?> testClass) throws Exception {
        Parameter parameter = testClass
                .getMethod("safariTest", SafariDriver.class).getParameters()[0];
        Optional<Object> testInstance = Optional.of(testClass.newInstance());
        SafariOptions safariOptions = (SafariOptions) annotationsReader
                .getOptions(parameter, testInstance);

        assertTrue(safariOptions.getUseCleanSession());
        assertFalse(safariOptions.getUseTechnologyPreview());
    }
}
