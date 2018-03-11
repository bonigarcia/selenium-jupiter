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

import static io.github.bonigarcia.SeleniumJupiter.config;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.invoke.MethodHandles.lookup;
import static org.openqa.selenium.Platform.ANY;
import static org.slf4j.LoggerFactory.getLogger;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;

import io.github.bonigarcia.handler.ChromeDriverHandler;
import io.github.bonigarcia.handler.DriverHandler;
import io.github.bonigarcia.handler.FirefoxDriverHandler;
import io.github.bonigarcia.handler.OperaDriverHandler;

/**
 * Enumeration for Selenoid browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public enum BrowserType {

    CHROME, FIREFOX, OPERA;

    final Logger log = getLogger(lookup().lookupClass());

    String dockerImage;
    String path;
    DriverHandler driverHandler;
    String optionsKey;
    DesiredCapabilities capabilities;

    public void init() {
        switch (this) {
        case FIREFOX:
            dockerImage = config().getFirefoxImageFormat();
            path = config().getFirefoxPath();
            driverHandler = new FirefoxDriverHandler();
            optionsKey = FirefoxOptions.FIREFOX_OPTIONS;
            capabilities = new DesiredCapabilities("firefox", "", ANY);
            break;
        case OPERA:
            dockerImage = config().getOperaImageFormat();
            path = config().getOperaPath();
            driverHandler = new OperaDriverHandler();
            optionsKey = OperaOptions.CAPABILITY;
            capabilities = new DesiredCapabilities("operablink", "", ANY);
            break;
        case CHROME:
        default:
            dockerImage = config().getChromeImageFormat();
            path = config().getChromePath();
            driverHandler = new ChromeDriverHandler();
            optionsKey = ChromeOptions.CAPABILITY;
            capabilities = new DesiredCapabilities("chrome", "", ANY);
            break;
        }
    }

    public String getNextVersion(String version, String latestVersion) {
        int iVersion = version.indexOf('.');
        iVersion = iVersion != -1 ? iVersion : version.length();
        int nextVersionInt = parseInt(version.substring(0, iVersion)) + 1;

        int iLatestVersion = latestVersion.indexOf('.');
        iLatestVersion = iLatestVersion != -1 ? iLatestVersion
                : latestVersion.length();
        int latestVersionInt = parseInt(
                latestVersion.substring(0, iLatestVersion)) + 1;

        if (nextVersionInt > latestVersionInt) {
            return null;
        }
        return String.valueOf(nextVersionInt) + ".0";
    }

    public int compareVersions(String v1, String v2) {
        String[] v1split = v1.split("\\.");
        String[] v2split = v2.split("\\.");
        int length = max(v1split.length, v2split.length);
        for (int i = 0; i < length; i++) {
            int v1Part = i < v1split.length ? parseInt(v1split[i]) : 0;
            int v2Part = i < v2split.length ? parseInt(v2split[i]) : 0;
            if (v1Part < v2Part) {
                return -1;
            }
            if (v1Part > v2Part) {
                return 1;
            }
        }
        return 0;
    }

    public String getPath() {
        return path;
    }

    public DriverHandler getDriverHandler() {
        return driverHandler;
    }

    public String getOptionsKey() {
        return optionsKey;
    }

    public String getDockerImage(String version) {
        return String.format(getDockerImage(), version);
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public DesiredCapabilities getCapabilities() {
        return capabilities;
    }

}
