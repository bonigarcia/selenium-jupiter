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
import static io.github.bonigarcia.SeleniumJupiter.config;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
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

    public DockerBrowserConfig(List<String> envs) {

        if (config().isBrowserListFromDockerHub()) {
            try {
                initBrowserConfigFromDockerHub(envs);
            } catch (Exception e) {
                log.warn(
                        "There was an error in browser initilization from Docker hub"
                                + " ... using properties values instead");
                initBrowserConfigFromProperties(envs);
            }
        } else {
            initBrowserConfigFromProperties(envs);
        }

        chrome.addBrowser("beta", new Browser(config().getChromeBetaImage(),
                config().getChromeBetaPath(), envs));
        chrome.addBrowser("unstable",
                new Browser(config().getChromeUnstableImage(),
                        config().getChromeUnstablePath(), envs));

        firefox.addBrowser("beta", new Browser(config().getFirefoxBetaImage(),
                config().getFirefoxBetaPath(), envs));
        firefox.addBrowser("unstable",
                new Browser(config().getFirefoxUnstableImage(),
                        config().getFirefoxUnstablePath(), envs));
    }

    public void initBrowserConfigFromDockerHub(List<String> envs)
            throws IOException {
        DockerHubService dockerHubService = new DockerHubService();
        List<DockerHubTag> listTags = dockerHubService.listTags();
        chrome = getBrowserConfigFromDockerHub(CHROME, listTags, envs);
        firefox = getBrowserConfigFromDockerHub(FIREFOX, listTags, envs);
        operablink = getBrowserConfigFromDockerHub(OPERA, listTags, envs);
    }

    public void initBrowserConfigFromProperties(List<String> envs) {
        chrome = getBrowserConfigFromProperties(CHROME, envs);
        firefox = getBrowserConfigFromProperties(FIREFOX, envs);
        operablink = getBrowserConfigFromProperties(OPERA, envs);
    }

    public BrowserConfig getBrowserConfigFromDockerHub(BrowserType browserType,
            List<DockerHubTag> dockerHubTags, List<String> envs) {
        List<String> browserList = null;
        String latestVersion = null;
        browserType.init();

        VersionComparator versionComparator = new VersionComparator();
        switch (browserType) {
        case FIREFOX:
            final String firefoxPreffix = "firefox_";
            browserList = dockerHubTags.stream()
                    .filter(p -> p.getName().startsWith(firefoxPreffix))
                    .map(p -> p.getName().replace(firefoxPreffix, ""))
                    .sorted(versionComparator::compare).collect(toList());
            latestVersion = browserList.get(browserList.size() - 1);
            break;
        case OPERA:
            final String operaPreffix = "opera_";
            browserList = dockerHubTags.stream()
                    .filter(p -> p.getName().startsWith(operaPreffix))
                    .map(p -> p.getName().replace(operaPreffix, ""))
                    .sorted(versionComparator::compare).skip(1)
                    .collect(toList());
            latestVersion = browserList.get(browserList.size() - 1);
            break;
        case CHROME:
        default:
            final String chromePreffix = "chrome_";
            browserList = dockerHubTags.stream()
                    .filter(p -> p.getName().startsWith(chromePreffix))
                    .map(p -> p.getName().replace(chromePreffix, ""))
                    .sorted(versionComparator::compare).collect(toList());
            latestVersion = browserList.get(browserList.size() - 1);
            break;
        }

        BrowserConfig browserConfig = new BrowserConfig(latestVersion);
        for (String version : browserList) {
            browserConfig.addBrowser(version,
                    new Browser(format(browserType.getDockerImage(), version),
                            browserType.getPath(), envs));
        }

        return browserConfig;
    }

    private BrowserConfig getBrowserConfigFromProperties(
            BrowserType browserType, List<String> envs) {
        String firstVersion = null;
        String latestVersion = null;
        browserType.init();
        switch (browserType) {
        case FIREFOX:
            firstVersion = config().getFirefoxFirstVersion();
            latestVersion = config().getFirefoxLatestVersion();
            break;
        case OPERA:
            firstVersion = config().getOperaFirstVersion();
            latestVersion = config().getOperaLatestVersion();
            break;
        case CHROME:
        default:
            firstVersion = config().getChromeFirstVersion();
            latestVersion = config().getChromeLatestVersion();
            break;
        }

        BrowserConfig browserConfig = new BrowserConfig(latestVersion);
        String version = firstVersion;
        do {
            browserConfig.addBrowser(version,
                    new Browser(format(browserType.getDockerImage(), version),
                            browserType.getPath(), envs));
            if (version.equals(latestVersion)) {
                break;
            }
            version = browserType.getNextVersion(version, latestVersion);
        } while (version != null);

        return browserConfig;
    }

    public BrowserConfig getBrowserConfig(BrowserType browser) {
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
        String port = config().getSelenoidPort();
        String path;
        Tmpfs tmpfs = new Tmpfs();
        List<String> env = new ArrayList<>();

        public Browser(String image, String path, List<String> envs) {
            this.image = image;
            this.path = path;
            this.env = envs;
        }

        public String getImage() {
            return image;
        }
    }

    public static class Tmpfs {
        @SerializedName("/tmp")
        String tmp = "size=" + config().getSelenoidTmpfsSize();
    }

}
