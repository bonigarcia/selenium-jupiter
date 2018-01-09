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
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toList;
import static org.openqa.selenium.Platform.ANY;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;

import io.github.bonigarcia.DockerBrowserConfig.Browser;
import io.github.bonigarcia.DockerBrowserConfig.BrowserConfig;
import io.github.bonigarcia.DockerHubTags.DockerHubTag;
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
    String latestVersion;
    String firstVersion;
    DriverHandler driverHandler;
    String optionsKey;

    public BrowserConfig getBrowserConfigFromDockerHub(
            List<DockerHubTag> dockerHubTags) {
        List<String> browserList = null;
        switch (this) {
        case FIREFOX:
            final String firefoxPreffix = "firefox_";
            browserList = dockerHubTags.stream()
                    .filter(p -> p.getName().startsWith(firefoxPreffix))
                    .map(p -> p.getName().replace(firefoxPreffix, ""))
                    .sorted(this::compareVersions).collect(toList());
            firstVersion = browserList.get(0);
            latestVersion = browserList.get(browserList.size() - 1);
            dockerImage = getString("sel.jup.firefox.image.format");
            driverHandler = new FirefoxDriverHandler();
            optionsKey = FirefoxOptions.FIREFOX_OPTIONS;
            break;
        case OPERA:
            final String operaPreffix = "opera_";
            browserList = dockerHubTags.stream()
                    .filter(p -> p.getName().startsWith(operaPreffix))
                    .map(p -> p.getName().replace(operaPreffix, ""))
                    .sorted(this::compareVersions).skip(1).collect(toList());
            firstVersion = browserList.get(0);
            latestVersion = browserList.get(browserList.size() - 1);
            dockerImage = getString("sel.jup.opera.image.format");
            driverHandler = new OperaDriverHandler();
            optionsKey = OperaOptions.CAPABILITY;
            break;
        case CHROME:
        default:
            final String chromePreffix = "chrome_";
            browserList = dockerHubTags.stream()
                    .filter(p -> p.getName().startsWith(chromePreffix))
                    .map(p -> p.getName().replace(chromePreffix, ""))
                    .sorted(this::compareVersions).collect(toList());
            firstVersion = browserList.get(0);
            latestVersion = browserList.get(browserList.size() - 1);
            dockerImage = getString("sel.jup.chrome.image.format");
            driverHandler = new ChromeDriverHandler();
            optionsKey = ChromeOptions.CAPABILITY;
            break;
        }

        BrowserConfig browserConfig = new BrowserConfig(latestVersion);
        for (String version : browserList) {
            browserConfig.addBrowser(version,
                    new Browser(format(dockerImage, version)));
        }

        return browserConfig;
    }

    public BrowserConfig getBrowserConfigFromProperties() {
        switch (this) {
        case FIREFOX:
            firstVersion = getString("sel.jup.firefox.first.version");
            latestVersion = getString("sel.jup.firefox.latest.version");
            dockerImage = getString("sel.jup.firefox.image.format");
            driverHandler = new FirefoxDriverHandler();
            optionsKey = FirefoxOptions.FIREFOX_OPTIONS;
            break;
        case OPERA:
            firstVersion = getString("sel.jup.opera.first.version");
            latestVersion = getString("sel.jup.opera.latest.version");
            dockerImage = getString("sel.jup.opera.image.format");
            driverHandler = new OperaDriverHandler();
            optionsKey = OperaOptions.CAPABILITY;
            break;
        case CHROME:
        default:
            firstVersion = getString("sel.jup.chrome.first.version");
            latestVersion = getString("sel.jup.chrome.latest.version");
            dockerImage = getString("sel.jup.chrome.image.format");
            driverHandler = new ChromeDriverHandler();
            optionsKey = ChromeOptions.CAPABILITY;
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

    public DesiredCapabilities getCapabilities() {
        switch (this) {
        case FIREFOX:
            return new DesiredCapabilities("firefox", "", ANY);
        case OPERA:
            return new DesiredCapabilities("operablink", "", ANY);
        case CHROME:
        default:
            return new DesiredCapabilities("chrome", "", ANY);
        }
    }

    public DriverHandler getDriverHandler() {
        return driverHandler;
    }

    public String getOptionsKey() {
        return optionsKey;
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

}
