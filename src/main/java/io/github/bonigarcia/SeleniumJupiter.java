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
package io.github.bonigarcia;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Scanner;

import org.openqa.selenium.WebDriver;
//import org.junit.jupiter.api.TestTemplate;
//import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.github.bonigarcia.config.Config;
import io.github.bonigarcia.handler.DockerDriverHandler;

/**
 * Main class for interactive mode.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumJupiter {

    static final Logger log = getLogger(lookup().lookupClass());

    protected static Config config;

    public static synchronized Config config() {
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    public static void main(String[] args) {
        String validBrowsers = "chrome|firefox|opera";
        if (args.length <= 0) {
            log.error("Usage: SeleniumJupiter browserName <version>",
                    validBrowsers);
            log.error("\t...where");
            log.error("\tbrowserName = {}", validBrowsers);
            log.error("\tversion = optional version (latest if empty)");
        } else {
            String browser = args[0];
            String version = "";
            String versionMessage = "(latest)";
            if (args.length > 1) {
                version = args[1];
                versionMessage = version;
            }
            log.info("Using SeleniumJupiter to execute {} {} in Docker",
                    browser, versionMessage);
            try {
                config().setVnc(true);
                DockerDriverHandler dockerDriverHandler = new DockerDriverHandler();

                BrowserType browserType = BrowserType
                        .valueOf(browser.toUpperCase());
                browserType.init();

                WebDriver webdriver = dockerDriverHandler.resolve(browserType,
                        version);

                log.info("Press ENTER to exit");
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
                scanner.close();

                webdriver.quit();
                dockerDriverHandler.cleanup();
                dockerDriverHandler.close();

            } catch (Exception e) {
                log.error("Exception trying to execute {} {} in Docker",
                        browser, versionMessage, e);
            }
        }
    }

}
