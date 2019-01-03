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

import static io.github.bonigarcia.BrowserType.ANDROID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.bonigarcia.BrowserInstance;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.config.Config;
import io.github.bonigarcia.handler.DockerDriverHandler;

public class AndroidBrowserTest {

    Config config = new Config();
    BrowserInstance android = new BrowserInstance(config, ANDROID);

    @ParameterizedTest
    @ValueSource(strings = { "5.0.1", "9.0" })
    void testAndroidVersions(String version) throws Exception {
        DockerDriverHandler dockerDriverHandler = new DockerDriverHandler(
                config, android, version);
        String androidUrl = dockerDriverHandler.startAndroidBrowser(version,
                config.getAndroidDeviceName());
        assertThat(androidUrl, notNullValue());
    }

    @Test
    void testAndroidWrongVersion() throws Exception {
        config.setAndroidDefaultVersion("");
        DockerDriverHandler dockerDriverHandler = new DockerDriverHandler(
                config, android, "");
        assertThrows(SeleniumJupiterException.class, () -> {
            dockerDriverHandler.startAndroidBrowser("", "");
        });
    }

}
