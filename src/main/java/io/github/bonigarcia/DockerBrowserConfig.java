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

import static io.github.bonigarcia.BrowserType.CHROME;
import static io.github.bonigarcia.BrowserType.FIREFOX;
import static io.github.bonigarcia.BrowserType.OPERA;
import static io.github.bonigarcia.SeleniumJupiter.getBoolean;
import static io.github.bonigarcia.SeleniumJupiter.getString;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.google.gson.annotations.SerializedName;

import io.github.bonigarcia.DockerHubTags.DockerHubTag;

/**
 * Enumeration for Selenoid browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class DockerBrowserConfig {

    final transient Logger log = getLogger(lookup().lookupClass());

    BrowserConfig chrome;
    BrowserConfig firefox;
    BrowserConfig operablink;

    public DockerBrowserConfig() {
        if (!getBoolean("sel.jup.browser.list.from.docker.hub")) {
            initBrowserConfigFromProperties();
        } else {
            try {
                initBrowserConfigFromDockerHub();
            } catch (Exception e) {
                log.warn(
                        "There was an error in browser initilization from Docker hub"
                                + " ... using properties values instead");
                initBrowserConfigFromProperties();
            }
        }
    }

    public void initBrowserConfigFromDockerHub() throws IOException {
        DockerHubService dockerHubService = new DockerHubService();
        List<DockerHubTag> listTags = dockerHubService.listTags();
        chrome = CHROME.getBrowserConfigFromDockerHub(listTags);
        firefox = FIREFOX.getBrowserConfigFromDockerHub(listTags);
        operablink = OPERA.getBrowserConfigFromDockerHub(listTags);
    }

    public void initBrowserConfigFromProperties() {
        chrome = CHROME.getBrowserConfigFromProperties();
        firefox = FIREFOX.getBrowserConfigFromProperties();
        operablink = OPERA.getBrowserConfigFromProperties();
    }

    public BrowserConfig getBrowser(BrowserType browser) {
        switch (browser) {
        case FIREFOX:
            return firefox;
        case OPERA:
            return operablink;
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
        String path;
        Tmpfs tmpfs = new Tmpfs();

        public Browser(String image, String path) {
            this.image = image;
            this.path = path;
        }

        public String getImage() {
            return image;
        }
    }

    public static class Tmpfs {
        @SerializedName("/tmp")
        String tmp = "size=" + getString("sel.jup.selenoid.tmpfs.size");
    }

}
