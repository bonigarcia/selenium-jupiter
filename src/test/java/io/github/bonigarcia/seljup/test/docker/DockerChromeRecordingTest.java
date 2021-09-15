/*
 * (C) Copyright 2017 Boni Garcia (https://bonigarcia.github.io/)
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

//tag::snippet-in-doc[]
import static io.github.bonigarcia.seljup.BrowserType.CHROME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
class DockerChromeRecordingTest {

    static SessionId sessionId;

    @AfterEach
    static void teardown() {
        File[] files = new File(".").listFiles();
        Optional<File> recording = Arrays.stream(files).filter(
                f -> f.getName().endsWith(sessionId.toString() + ".mp4"))
                .findFirst();
        assertThat(recording).isNotEmpty();
        recording.get().delete();
    }

    @Test
    void recordingTest(
            @DockerBrowser(type = CHROME, recording = true) RemoteWebDriver driver)
            throws InterruptedException {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.getTitle()).contains("Selenium WebDriver");

        Thread.sleep(Duration.ofSeconds(3).toMillis());

        sessionId = driver.getSessionId();
    }

}
//end::snippet-in-doc[]
