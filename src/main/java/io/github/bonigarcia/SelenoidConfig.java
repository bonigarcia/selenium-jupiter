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
import static java.lang.String.valueOf;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Utilities related with Selenoid.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class SelenoidConfig {

    final Logger log = getLogger(lookup().lookupClass());

    public final static int DOCKER_CONTAINER_PORT = 4444;

    final static String CHROME_DOCKER_IMAGE = "selenoid/vnc:chrome_%s";
    final static String FIREFOX_DOCKER_IMAGE = "selenoid/vnc:firefox_%s";
    final static String OPERA_DOCKER_IMAGE = "selenoid/vnc:opera_%s";

    final static String CHROME_FIRST_VERSION = "48.0";
    final static String FIREFOX_FIRST_VERSION = "3.6";
    final static String OPERA_FIRST_VERSION = "33.0";

    Browsers browsers;

    public SelenoidConfig() {
        // Initialize from properties
        String chromeLatestVersion = getString("sel.jup.chrome.latest.version");
        String firefoxLatestVersion = getString(
                "sel.jup.firefox.latest.version");
        String operaLatestVersion = getString("sel.jup.opera.latest.version");

        BrowserConfig chromeConfig = getBrowserConfig(CHROME_FIRST_VERSION,
                chromeLatestVersion, CHROME_DOCKER_IMAGE);
        BrowserConfig firefoxConfig = getBrowserConfig(FIREFOX_FIRST_VERSION,
                firefoxLatestVersion, FIREFOX_DOCKER_IMAGE);
        BrowserConfig operaConfig = getBrowserConfig(OPERA_FIRST_VERSION,
                operaLatestVersion, OPERA_DOCKER_IMAGE);

        browsers = new Browsers(chromeConfig, firefoxConfig, operaConfig);
    }

    public String getBrowsersJsonAsString() {
        return new Gson().toJson(browsers);
    }

    private BrowserConfig getBrowserConfig(String firstVersion,
            String latestVersion, String dockerImage) {
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

    public String getNextVersion(String version, String latestVersion) {
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
        return valueOf(nextVersionInt) + ".0";
    }

    public String getChromeImageFromVersion(String version) {
        Map<String, Browser> versions = browsers.getChrome().getVersions();

        if (versions.containsKey(version)) {
            return versions.get(version).getImage();
        }

        for (String v : versions.keySet()) {
            if (version.startsWith(v)) {
                return versions.get(v).getImage();
            }
        }

        throw new SeleniumJupiterException(
                "Version " + version + " is not valid for Chrome");
    }

    public String getLatestChromeImage() {
        return format(CHROME_DOCKER_IMAGE,
                browsers.getChrome().getDefaultBrowser());
    }

    public String getLatestFirefoxImage() {
        return format(FIREFOX_DOCKER_IMAGE,
                browsers.getFirefox().getDefaultBrowser());
    }

    public String getLatestOperaImage() {
        return format(OPERA_DOCKER_IMAGE,
                browsers.getOpera().getDefaultBrowser());
    }

    class Browsers {
        BrowserConfig chrome;
        BrowserConfig firefox;
        BrowserConfig opera;

        public Browsers(BrowserConfig chrome, BrowserConfig firefox,
                BrowserConfig opera) {
            this.chrome = chrome;
            this.firefox = firefox;
            this.opera = opera;
        }

        public BrowserConfig getChrome() {
            return chrome;
        }

        public BrowserConfig getFirefox() {
            return firefox;
        }

        public BrowserConfig getOpera() {
            return opera;
        }

    }

    class BrowserConfig {
        @SerializedName("default")
        String defaultBrowser;
        Map<String, Browser> versions;

        public BrowserConfig(String defaultBrowser) {
            this.defaultBrowser = defaultBrowser;
            this.versions = new TreeMap<>();
        }

        public void addBrowser(String version, Browser browser) {
            this.versions.put(version, browser);
        }

        public String getDefaultBrowser() {
            return defaultBrowser;
        }

        public Map<String, Browser> getVersions() {
            return versions;
        }

    }

    class Browser {
        String image;
        String port = String.valueOf(DOCKER_CONTAINER_PORT);

        public Browser(String image) {
            this.image = image;
        }

        public String getImage() {
            return image;
        }

    }

}
