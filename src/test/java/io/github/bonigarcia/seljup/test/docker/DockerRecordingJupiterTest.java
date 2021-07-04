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
package io.github.bonigarcia.seljup.test.docker;

import static io.github.bonigarcia.seljup.BrowserType.CHROME;
import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@TestInstance(PER_CLASS)
class DockerRecordingJupiterTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    final Logger log = getLogger(lookup().lookupClass());

    File recordingFile;

    @BeforeEach
    void setup() {
        seleniumJupiter.getConfig().setRecording(true);
    }

    @AfterAll
    void teardown() {
        if (recordingFile != null) {
            assertTrue(recordingFile.exists());
            log.info("Deleting recording {} ... {}", recordingFile,
                    recordingFile.delete());
        }
    }

    @Test
    void testLatest(
            @DockerBrowser(type = CHROME, version = "88.0") RemoteWebDriver arg0) {
        arg0.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(arg0.getTitle()).contains("JUnit 5 extension for Selenium");

        recordingFile = new File(
                "testLatest_arg0_CHROME_88.0_" + arg0.getSessionId() + ".mp4");
    }

}
