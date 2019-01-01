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

import static io.github.bonigarcia.BrowserType.CHROME;
import static io.github.bonigarcia.BrowserType.FIREFOX;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumExtension;

public class MixedDockerChromeJupiterTest {

    static final int NUM_BROWSERS = 3;

    @RegisterExtension
    static SeleniumExtension seleniumExtension = new SeleniumExtension();

    final Logger log = getLogger(lookup().lookupClass());

    @BeforeEach
    void setup() {
        seleniumExtension.getConfig().setVnc(true);
    }

    @Test
    public void testMixed(@DockerBrowser(type = CHROME) RemoteWebDriver chrome,
            @DockerBrowser(type = CHROME, size = NUM_BROWSERS) List<RemoteWebDriver> driverList,
            @DockerBrowser(type = FIREFOX) RemoteWebDriver firefox)
            throws InterruptedException {

        ExecutorService executorService = newFixedThreadPool(NUM_BROWSERS + 2);
        CountDownLatch latch = new CountDownLatch(NUM_BROWSERS + 2);

        driverList.forEach((driver) -> {
            exercise(executorService, latch, driver);
        });
        exercise(executorService, latch, chrome);
        exercise(executorService, latch, firefox);

        latch.await(50, SECONDS);

        // Thread.sleep(30000);

        executorService.shutdown();
    }

    private void exercise(ExecutorService executorService, CountDownLatch latch,
            RemoteWebDriver driver) {
        executorService.submit(() -> {
            try {
                log.info("Session id {}",
                        ((RemoteWebDriver) driver).getSessionId());
                driver.get("https://bonigarcia.github.io/selenium-jupiter/");
                assertThat(driver.getTitle(),
                        containsString("JUnit 5 extension for Selenium"));
            } finally {
                latch.countDown();
            }
        });
    }

}
