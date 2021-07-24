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
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

class DockerRecordingConfigJupiterTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    final Logger log = getLogger(lookup().lookupClass());

    File recordingFile;

    @BeforeAll
    static void setup() {
        seleniumJupiter.getConfig().enableRecording();
    }

    @AfterEach
    void teardown() {
        if (recordingFile != null) {
            assertThat(recordingFile).exists();
            log.info("Deleting recording {} ... {}", recordingFile,
                    recordingFile.delete());
        }
    }

    @Test
    void test(@DockerBrowser(type = CHROME) RemoteWebDriver driver)
            throws InterruptedException {
        driver.get("https://bonigarcia.github.io/selenium-jupiter/");
        assertThat(driver.getTitle())
                .contains("JUnit 5 extension for Selenium");

        // Uncomment this line to get a longer recording
        // Thread.sleep(5000);

        recordingFile = new File("chrome_" + driver.getSessionId() + ".mp4");
    }

}
