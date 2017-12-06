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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import io.github.bonigarcia.handler.EdgeDriverHandler;
import io.github.bonigarcia.test.advance.EdgeWithOptionsJupiterTest;
import io.github.bonigarcia.test.mockito.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EdgeAnnotationReaderTest {

    @InjectMocks
    EdgeDriverHandler annotationsReader;

    @Test
    void testEdgeOptions() throws Exception {
        Parameter parameter = EdgeWithOptionsJupiterTest.class
                .getMethod("edgeTest", EdgeDriver.class).getParameters()[0];
        EdgeOptions edgeOptions = annotationsReader.getEdgeOptions(parameter,
                Optional.empty());

        assertEquals("eager", edgeOptions.getCapability("pageLoadStrategy"));
    }

}
