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
package io.github.bonigarcia.seljup;

import static io.github.bonigarcia.seljup.CloudType.NONE;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.join;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Scanner;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.config.Config;
import io.github.bonigarcia.seljup.handler.DockerDriverHandler;

/**
 * Main class for interactive mode.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumJupiter {

    static final Logger log = getLogger(lookup().lookupClass());

    static Config config = new Config();
    static AnnotationsReader annotationsReader = new AnnotationsReader();
    static InternalPreferences preferences = new InternalPreferences(config);

    public static void main(String[] args) {
        String validBrowsers = "chrome|firefox|opera|android";
        if (args.length <= 0) {
            logCliError(validBrowsers);

        } else {
            String arg = args[0];
            if (arg.equalsIgnoreCase("server")) {
                startServer(args);
            } else if (arg.equalsIgnoreCase("clear-preferences")) {
                new InternalPreferences(config).clear();
            } else {
                resolveLocal(args);
            }
        }
    }

    private static void resolveLocal(String[] args) {
        String browser = args[0];
        String version = "";
        String deviceName = "";
        String versionMessage = "(latest)";
        if (args.length > 1) {
            version = args[1];
            versionMessage = version;
        }
        if (args.length > 2) {
            deviceName = join(" ", copyOfRange(args, 2, args.length));
        }

        log.info("Using SeleniumJupiter to execute {} {} in Docker", browser,
                versionMessage);

        try {
            config.setVnc(true);
            config.setBrowserSessionTimeoutDuration("99h0m0s");

            BrowserInstance browserInstance = new BrowserInstance(config,
                    annotationsReader,
                    BrowserType.valueOf(browser.toUpperCase()), NONE, empty(),
                    emptyList());
            DockerDriverHandler dockerDriverHandler = new DockerDriverHandler(
                    config, browserInstance, version, preferences);

            WebDriver webdriver = dockerDriverHandler.resolve(browserInstance,
                    version, deviceName, config.getDockerServerUrl(), true);

            getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    cleanContainers(dockerDriverHandler, webdriver);
                }
            });

            log.info("Press ENTER to exit");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            scanner.close();

            cleanContainers(dockerDriverHandler, webdriver);

        } catch (Exception e) {
            log.error("Exception trying to execute {} {} in Docker", browser,
                    versionMessage, e);
        }
    }

    private static void cleanContainers(DockerDriverHandler dockerDriverHandler,
            WebDriver webdriver) {
        if (webdriver != null) {
            webdriver.quit();
        }
        if (dockerDriverHandler != null) {
            dockerDriverHandler.cleanup();
            dockerDriverHandler.close();
        }
    }

    private static void startServer(String[] args) {
        int port = config.getServerPort();
        if (args.length > 1 && isNumeric(args[1])) {
            port = parseInt(args[1]);
        }
        new Server(port);
    }

    private static void logCliError(String validBrowsers) {
        log.error("There are 3 options to run Selenium-Jupiter CLI");
        log.error("1. Selenium-Jupiter used to get VNC sessions of browsers:");
        log.error("\tSeleniumJupiter browserName <version> <deviceName>");
        log.error("\t...where:");
        log.error("\tbrowserName = {}", validBrowsers);
        log.error("\tversion = optional version (latest by default)");
        log.error("\tdeviceName = Device name (only for Android)");
        log.error("\t(where browserName={})", validBrowsers);

        log.error("2. Selenium-Jupiter as a server:");
        log.error("\tSelenium-Jupiter server <port>");
        log.error("\t(where default port is 4042)");

        log.error(
                "3. To clear previously Docker image versions (as Java preferences):");
        log.error("\tSelenium-Jupiter clear-preferences");
    }

}
