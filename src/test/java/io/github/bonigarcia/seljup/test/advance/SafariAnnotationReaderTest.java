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
package io.github.bonigarcia.seljup.test.advance;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.handler.DriverHandler;
import io.github.bonigarcia.seljup.handler.SafariDriverHandler;

class SafariAnnotationReaderTest {

    DriverHandler annotationsReader = new SafariDriverHandler(null, null, null,
            new AnnotationsReader());

    static Stream<Class<?>> testClassProvider() {
        return Stream.of(SafariWithGlobalOptionsJupiterTest.class);
    }

    @ParameterizedTest
    @MethodSource("testClassProvider")
    void testSafariOptions(Class<?> testClass) throws Exception {
        Method method = testClass.getDeclaredMethod("safariTest",
                SafariDriver.class);
        method.setAccessible(true);
        Parameter parameter = method.getParameters()[0];
        Optional<Object> testInstance = Optional
                .of(testClass.getDeclaredConstructor().newInstance());
        SafariOptions safariOptions = (SafariOptions) annotationsReader
                .getOptions(parameter, testInstance);

        assertFalse(safariOptions.getUseTechnologyPreview());
    }

    @Test
    void testAnnotatedSafariOptionsIsSelectedOverOtherAnnotatedOptions()
            throws Exception {
        Optional<Object> testInstance = Optional
                .of(new ClassWithMultipleOptions());
        SafariOptions safariOptions = (SafariOptions) annotationsReader
                .getOptions(null, testInstance);
        assertThat(safariOptions, notNullValue());
    }
}
