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

import static io.github.bonigarcia.SeleniumJupiter.getString;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static org.openqa.selenium.remote.DesiredCapabilities.chrome;
import static org.openqa.selenium.remote.DesiredCapabilities.firefox;
import static org.openqa.selenium.remote.DesiredCapabilities.operaBlink;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.DockerBrowserConfig.Browser;
import io.github.bonigarcia.DockerBrowserConfig.BrowserConfig;

/**
 * Enumeration for Selenoid browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public enum SelenoidBrowser {

    CHROME, FIREFOX, OPERA;

    public final static int DOCKER_CONTAINER_PORT = 4444;

    final static String CHROME_DOCKER_IMAGE = "selenoid/vnc:chrome_%s";
    final static String FIREFOX_DOCKER_IMAGE = "selenoid/vnc:firefox_%s";
    final static String OPERA_DOCKER_IMAGE = "selenoid/vnc:opera_%s";

    final static String CHROME_FIRST_VERSION = "48.0";
    final static String FIREFOX_FIRST_VERSION = "3.6";
    final static String OPERA_FIRST_VERSION = "33.0";

    String dockerImage;
    Class<? extends RemoteWebDriver> driverClass;
    DesiredCapabilities capabilities;
    String latestVersion;
    String firstVersion;

    public BrowserConfig getBrowserConfigFromProp() {
        switch (this) {
        case FIREFOX:
            latestVersion = getString("sel.jup.firefox.latest.version");
            firstVersion = FIREFOX_FIRST_VERSION;
            dockerImage = FIREFOX_DOCKER_IMAGE;
            driverClass = DockerFirefoxDriver.class;
            capabilities = firefox();
            break;
        case OPERA:
            latestVersion = getString("sel.jup.opera.latest.version");
            firstVersion = OPERA_FIRST_VERSION;
            dockerImage = OPERA_DOCKER_IMAGE;
            driverClass = DockerOperaDriver.class;
            capabilities = operaBlink();
            break;
        default:
        case CHROME:
            latestVersion = getString("sel.jup.chrome.latest.version");
            firstVersion = CHROME_FIRST_VERSION;
            dockerImage = CHROME_DOCKER_IMAGE;
            driverClass = DockerChromeDriver.class;
            capabilities = chrome();
            break;
        }

        BrowserConfig browserConfig = new BrowserConfig(latestVersion);
        String version = firstVersion;
        do {
            browserConfig.addBrowser(version,
                    new Browser(format(dockerImage, version)));
            if (version.equals(latestVersion)) {
                break;
            }
            version = getNextVersion(version, latestVersion);
        } while (version != null);

        return browserConfig;
    }

    public String getDockerImage(String version) {
        return String.format(getDockerImage(), version);
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public Class<? extends RemoteWebDriver> getDriverClass() {
        return driverClass;
    }

    public DesiredCapabilities getCapabilities() {
        return capabilities;
    }

    public static String getNextVersion(String version, String latestVersion) {
        int iVersion = version.indexOf(".");
        iVersion = iVersion != -1 ? iVersion : version.length();
        int nextVersionInt = parseInt(version.substring(0, iVersion)) + 1;

        int iLatestVersion = latestVersion.indexOf(".");
        iLatestVersion = iLatestVersion != -1 ? iLatestVersion
                : latestVersion.length();
        int latestVersionInt = parseInt(
                latestVersion.substring(0, iLatestVersion)) + 1;

        if (nextVersionInt > latestVersionInt) {
            return null;
        }
        return String.valueOf(nextVersionInt) + ".0";
    }

    public static int getDockerContainerPort() {
        return DOCKER_CONTAINER_PORT;
    }

}
