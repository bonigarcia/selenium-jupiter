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
package io.github.bonigarcia.seljup.test.annotations;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.Options;
import io.github.bonigarcia.seljup.SeleniumJupiterException;

public class AnnotationsReaderTest {

    AnnotationsReader annotationsReader = new AnnotationsReader();
    Optional<Object> testInstance = Optional.of(new ClassWithOptions());

    @Test
    void testThrowsExceptionWithNullTestInstance() throws Exception {
        Object optionsFromAnnotatedField = annotationsReader
                .getOptionsFromAnnotatedField(null, Options.class);
        assertThat(optionsFromAnnotatedField, nullValue());
    }

    @Test
    void testThrowsExceptionWithNullCapabilitiesClass() throws Exception {
        assertThrows(SeleniumJupiterException.class, () -> {
            annotationsReader.getOptionsFromAnnotatedField(testInstance,
                    Options.class, null);
        }, "The parameter capabilitiesClass must not be null.");
    }

    @Test
    void testGetsNullOptionsIfEmptyTestInstance() throws Exception {
        Object options = annotationsReader
                .getOptionsFromAnnotatedField(Optional.empty(), Options.class);
        assertThat(options, nullValue());
    }

    @Test
    void testGetsFirstDeclaredOptionsFromAnnotatedFieldWithoutTypeSpecied()
            throws Exception {
        Object options = annotationsReader
                .getOptionsFromAnnotatedField(testInstance, Options.class);
        assertThat(options, instanceOf(Integer.class));
    }

    @Test
    void testGetsNullOptionsFromAnnotatedFieldIfSpeciedTypeNotFound()
            throws Exception {
        Capabilities options = annotationsReader.getOptionsFromAnnotatedField(
                testInstance, Options.class, Capabilities.class);
        assertThat(options, nullValue());
    }

    @Test
    void testGetsOptionsFromAnnotatedFieldWithSpeciedType() throws Exception {
        CapabilitiesA optionsA = annotationsReader.getOptionsFromAnnotatedField(
                testInstance, Options.class, CapabilitiesA.class);
        assertThat(optionsA, notNullValue());
        assertThat(optionsA.getId(), equalTo("A"));

        CapabilitiesB optionsB = annotationsReader.getOptionsFromAnnotatedField(
                testInstance, Options.class, CapabilitiesB.class);
        assertThat(optionsB, notNullValue());
        assertThat(optionsB.getId(), equalTo("B"));
    }

    private static class ClassWithOptions {

        @Options
        Integer integer = 1;

        @Options
        CapabilitiesA capabilitiesA = new CapabilitiesA("A");

        @Options
        String string = "string";

        @Options
        CapabilitiesB capabilitiesB = new CapabilitiesB("B");

        @Options
        CapabilitiesA unreachable = new CapabilitiesA("unreachable");
    }

    private static class CapabilitiesA extends AbstractCapabilities {

        public CapabilitiesA(String id) {
            super(id);
        }
    }

    private static class CapabilitiesB extends AbstractCapabilities {

        public CapabilitiesB(String id) {
            super(id);
        }
    }

    private static abstract class AbstractCapabilities implements Capabilities {

        private static final Map<String, Object> capabilities = new HashMap<>();

        private final String id;

        public AbstractCapabilities(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public Map<String, Object> asMap() {
            return capabilities;
        }

        @Override
        public Object getCapability(String capabilityName) {
            return capabilities.get(capabilityName);
        }
    }
}
