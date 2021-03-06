/*
 * (C) Copyright 2019 Boni Garcia (http://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup.test.server;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.openqa.selenium.net.PortProber.findFreePort;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.SeleniumJupiter;

/**
 * Test interactive mode (from the shell).
 *
 * @author Boni Garcia
 * @since 2.1.0
 */
@TestInstance(PER_CLASS)
class ServerJupiterTest {

    final Logger log = getLogger(lookup().lookupClass());

    String serverPort;

    @BeforeAll
    void startServer() throws IOException {
        serverPort = String.valueOf(findFreePort());
        log.debug("Test is starting Selenium-Jupiter server at port {}",
                serverPort);
        SeleniumJupiter.main(new String[] { "server", serverPort });
    }

    @ParameterizedTest
    @MethodSource("capabilitesProvider")
    void testServer(Capabilities capabilities) throws MalformedURLException {
        String serverUrl = String.format("http://localhost:%s/wd/hub/",
                serverPort);
        RemoteWebDriver driver = new RemoteWebDriver(new URL(serverUrl),
                capabilities);

        assertNotNull(driver);
        driver.get("https://bonigarcia.github.io/selenium-jupiter/");

        String title = driver.getTitle();
        log.debug("The title is {}", title);
        assertThat(title).contains("JUnit 5 extension for Selenium");

        driver.quit();
        assertNull(driver.getSessionId());
    }

    static Stream<Capabilities> capabilitesProvider() {
        return Stream.of(new ChromeOptions(), new FirefoxOptions());
    }

}
