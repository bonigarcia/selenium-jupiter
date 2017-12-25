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
import static io.github.bonigarcia.BrowserType.CHROME;
import static io.github.bonigarcia.BrowserType.FIREFOX;
import static io.github.bonigarcia.BrowserType.OPERA;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * Enumeration for Selenoid browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class DockerBrowserConfig {

    BrowserConfig chrome;
    BrowserConfig firefox;
    BrowserConfig opera;

    public DockerBrowserConfig() {
        // By default, initialize from properties
        this.chrome = CHROME.getBrowserConfigFromProperties();
        this.firefox = FIREFOX.getBrowserConfigFromProperties();
        this.opera = OPERA.getBrowserConfigFromProperties();
    }

    public BrowserConfig getBrowser(BrowserType browser) {
        switch (browser) {
        case FIREFOX:
            return firefox;
        case OPERA:
            return opera;
        case CHROME:
        default:
            return chrome;
        }
    }

    public static class BrowserConfig {
        @SerializedName("default")
        String defaultBrowser;
        Map<String, Browser> versions;

        public BrowserConfig(String defaultBrowser) {
            this.defaultBrowser = defaultBrowser;
            this.versions = new HashMap<>();
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

    public static class Browser {
        String image;
        String port = getString("sel.jup.selenoid.port");

        public Browser(String image) {
            this.image = image;
        }

        public String getImage() {
            return image;
        }
    }

}
