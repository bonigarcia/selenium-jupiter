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
package io.github.bonigarcia.test.docker;

import static io.github.bonigarcia.SeleniumJupiter.config;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import io.github.bonigarcia.DockerContainer;
import io.github.bonigarcia.DockerService;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.handler.DockerDriverHandler;
import io.github.bonigarcia.test.mockito.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AndroidBrowserTest {

    @InjectMocks
    DockerDriverHandler dockerDriverHandler;

    @Mock
    DockerService dockerService;

    @Mock
    Map<String, DockerContainer> containerMap;

    @ParameterizedTest
    @ValueSource(strings = { "5.0.1", "5.1.1", "6.0", "7.0", "7.1.1" })
    void testAndroidVersions(String version) throws Exception {
        String androidUrl = dockerDriverHandler.startAndroidBrowser(version,
                config().getAndroidDeviceName());
        assertThat(androidUrl, notNullValue());
    }

    @Test
    void testAndroidWrongVersion() throws Exception {
        config().setAndroidDefaultVersion("");
        assertThrows(SeleniumJupiterException.class, () -> {
            dockerDriverHandler.startAndroidBrowser("", "");
        });
    }

}
