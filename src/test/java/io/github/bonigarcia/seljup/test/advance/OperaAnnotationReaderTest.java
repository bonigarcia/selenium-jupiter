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

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.handler.DriverHandler;
import io.github.bonigarcia.seljup.handler.OperaDriverHandler;

class OperaAnnotationReaderTest {

    DriverHandler annotationsReader = new OperaDriverHandler(null, null, null,
            new AnnotationsReader());

    static Stream<Class<?>> testClassProvider() {
        return Stream.of(OperaWithOptionsJupiterTest.class,
                OperaWithGlobalOptionsJupiterTest.class);
    }

    @Test
    void testOperaOptions() throws Exception {
        Parameter parameter = OperaWithOptionsJupiterTest.class
                .getMethod("operaTest", OperaDriver.class).getParameters()[0];
        OperaOptions operaOptions = (OperaOptions) annotationsReader
                .getOptions(parameter, empty());

        assertThat(operaOptions.asMap().get("operaOptions").toString(),
                containsString("binary"));
    }

    @Test
    void testAnnotatedOperaOptionsIsSelectedOverOtherAnnotatedOptions()
            throws Exception {
        Optional<Object> testInstance = Optional
                .of(new ClassWithMultipleOptions());
        OperaOptions operaOptions = (OperaOptions) annotationsReader
                .getOptions(null, testInstance);
        assertThat(operaOptions, notNullValue());
    }
}
