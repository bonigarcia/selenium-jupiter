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

import static io.github.bonigarcia.BrowserType.ANDROID;
import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.BrowserInstance;
import io.github.bonigarcia.InternalPreferences;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.config.Config;
import io.github.bonigarcia.handler.DockerDriverHandler;

@TestInstance(PER_CLASS)
public class AndroidForcedTimeoutJupiterTest {

    final Logger log = getLogger(lookup().lookupClass());

    DockerDriverHandler dockerDriverHandler;
    Config config = new Config();
    InternalPreferences preferences = new InternalPreferences(config);

    @BeforeAll
    void setup() {
        config.setVnc(true);
        config.setAndroidDeviceTimeoutSec(10);
    }

    @AfterEach
    void cleanup() {
        if (dockerDriverHandler != null) {
            dockerDriverHandler.cleanup();
        }
    }

    @Test
    void androidTimeoutTest() {
        assertThrows(SeleniumJupiterException.class, () -> {
            BrowserInstance android = new BrowserInstance(config, ANDROID);
            String version = "9.0";
            dockerDriverHandler = new DockerDriverHandler(config, android,
                    version, preferences);
            WebDriver driver = dockerDriverHandler.resolve(android, version,
                    "Samsung Galaxy S6", "", true);
            log.debug("WebDriver object: {}", driver);
        });
    }

}
