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
package io.github.bonigarcia;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URL;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.config.Config;

/**
 * Utility for instantiate WebDriver objects.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 3.0.0
 */
public class WebDriverCreator {

    final Logger log = getLogger(lookup().lookupClass());

    Config config;

    public WebDriverCreator(Config config) {
        this.config = config;
    }

    public WebDriver createRemoteWebDriver(URL hubUrl,
            Capabilities capabilities) {
        WebDriver webdriver = null;
        int waitTimeoutSec = getConfig().getRemoteWebdriverWaitTimeoutSec();
        int pollTimeSec = getConfig().getRemoteWebdriverPollTimeSec();
        long timeoutMs = currentTimeMillis() + SECONDS.toMillis(waitTimeoutSec);
        do {
            if (currentTimeMillis() > timeoutMs) {
                throw new SeleniumJupiterException(
                        "Timeout of " + waitTimeoutSec
                                + "  seconds creating WebDriver object");
            }
            try {
                log.debug("Creating WebDriver object for {} at {}",
                        capabilities.getBrowserName(), hubUrl);
                log.trace("Complete {}", capabilities);
                webdriver = new RemoteWebDriver(hubUrl, capabilities);
            } catch (Exception e1) {
                try {
                    log.warn(
                            "Exception creating WebDriver object {} ... retrying in {} second(s)",
                            e1.getClass().getSimpleName(), pollTimeSec);
                    sleep(SECONDS.toMillis(pollTimeSec));
                } catch (InterruptedException e2) {
                    log.warn("Interrupted exception creating WebDriver object",
                            e2);
                    currentThread().interrupt();
                }
            }

        } while (webdriver == null);

        log.trace("Created WebDriver object (session id {})",
                ((RemoteWebDriver) webdriver).getSessionId());

        return webdriver;
    }

    public Config getConfig() {
        return config;
    }

}
