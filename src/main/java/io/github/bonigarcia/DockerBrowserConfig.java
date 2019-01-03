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
import static java.lang.Integer.parseInt;
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
import io.github.bonigarcia.config.Config;

/**
 * Enumeration for Selenoid browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class DockerBrowserConfig {

    final transient Logger log = getLogger(lookup().lookupClass());
    static final transient String VERSION_SUFFIX = ".0";
    static final transient String LATEST = "latest";

    BrowserConfig chrome;
    BrowserConfig firefox;
    BrowserConfig operablink;
    transient Config config;
    transient String version;
    transient InternalPreferences preferences;

    public DockerBrowserConfig(List<String> envs, Config config,
            BrowserInstance browserInstance, String label) {
        this.config = config;
        this.preferences = new InternalPreferences(config);

        boolean isLatest = label == null || label.isEmpty()
                || label.equalsIgnoreCase(LATEST);
        boolean isBeta = label != null && label.equalsIgnoreCase("beta");
        boolean isUnstable = label != null
                && label.equalsIgnoreCase("unstable");

        if (isLatest) {
            version = getLatestVersion(browserInstance);
        } else if (label.startsWith(LATEST)) {
            version = getVersionFromLabel(browserInstance, label);
        } else if (!isBeta && !isUnstable && !label.endsWith(VERSION_SUFFIX)) {
            version = label + VERSION_SUFFIX;
        } else {
            version = label;
        }

        Browser browser = getBrowser(envs, browserInstance, isBeta, isUnstable);
        switch (browserInstance.getBrowserType()) {
        case FIREFOX:
            firefox = new BrowserConfig(version);
            firefox.addBrowser(version, browser);
            break;
        case OPERA:
            operablink = new BrowserConfig(version);
            operablink.addBrowser(version, browser);
            break;
        case CHROME:
        default:
            chrome = new BrowserConfig(version);
            chrome.addBrowser(version, browser);
            break;
        }
    }

    private Browser getBrowser(List<String> envs,
            BrowserInstance browserInstance, boolean isBeta,
            boolean isUnstable) {
        String dockerImage = format(browserInstance.getDockerImage(), version);
        String path = browserInstance.getPath();
        BrowserType browserType = browserInstance.getBrowserType();
        if (isBeta && browserType == CHROME) {
            dockerImage = getConfig().getChromeBetaImage();
            path = getConfig().getChromeBetaPath();
        } else if (isUnstable && browserType == CHROME) {
            dockerImage = getConfig().getChromeUnstableImage();
            path = getConfig().getChromeUnstablePath();
        } else if (isBeta && browserType == FIREFOX) {
            dockerImage = getConfig().getFirefoxBetaImage();
            path = getConfig().getFirefoxBetaPath();
        } else if (isUnstable && browserType == FIREFOX) {
            dockerImage = getConfig().getFirefoxUnstableImage();
            path = getConfig().getFirefoxUnstablePath();
        }
        Browser browser = new Browser(dockerImage, path, envs);
        return browser;
    }

    private String getLatestVersion(BrowserInstance browserInstance) {
        String latestVersion = null;
        if (config.isBrowserListFromDockerHub()) {
            // First seek in preferences
            String key = browserInstance.getBrowserName();
            String versionFromPreferences = preferences
                    .getValueFromPreferences(key);
            boolean versionInPreferences = versionFromPreferences != null
                    && !versionFromPreferences.isEmpty();
            if (versionInPreferences) {
                long expirationTime = preferences
                        .getExpirationTimeFromPreferences(key);
                String expirationDate = preferences.formatTime(expirationTime);
                log.trace(
                        "Version in preferences: {} (expiration date {}) (key {})",
                        versionFromPreferences, expirationDate, key);
                versionInPreferences &= preferences.checkValidity(key,
                        versionFromPreferences, expirationTime);
                if (versionInPreferences) {
                    log.trace(
                            "Using {} {} (latest value previously resolved, stored as Java preferences and valid until {})",
                            key, versionFromPreferences, expirationDate);
                    latestVersion = versionFromPreferences;
                }
            }
            if (!versionInPreferences) {
                try {
                    latestVersion = getLatestVersionFromDockerHub(
                            browserInstance);
                } catch (Exception e) {
                    log.warn(
                            "There was an error in browser initilization from Docker hub"
                                    + " ... using properties values instead");
                    latestVersion = getLatestVersionFromProperties(
                            browserInstance);
                }
            }
        } else {
            latestVersion = getLatestVersionFromProperties(browserInstance);
        }
        return latestVersion;
    }

    public String getVersionFromLabel(BrowserInstance browserInstance,
            String label) {
        int beforeVersion = Integer.parseInt(label.replace(LATEST + "-", ""));
        String latestVersion = getLatestVersion(browserInstance);
        String previousVersion = getPreviousVersion(beforeVersion,
                latestVersion);
        log.debug("Version {} for {} (latest version {}) = {}", label,
                browserInstance, latestVersion, previousVersion);
        return previousVersion;
    }

    private String getPreviousVersion(int beforeVersion, String latestVersion) {
        int iLatestVersion = latestVersion.indexOf('_') + 1;
        int jLatestVersion = latestVersion.indexOf('.');
        int latestVersionInt = parseInt(
                latestVersion.substring(iLatestVersion, jLatestVersion));
        if (beforeVersion > latestVersionInt) {
            return null;
        }
        return String.valueOf(latestVersionInt - beforeVersion) + ".0";
    }

    public String getLatestVersionFromDockerHub(BrowserInstance browserInstance)
            throws IOException {
        VersionComparator versionComparator = new VersionComparator();
        List<String> browserList = null;
        String latestVersion = null;
        DockerHubService dockerHubService = new DockerHubService(getConfig());
        List<DockerHubTag> dockerHubTags = dockerHubService.listTags();

        switch (browserInstance.getBrowserType()) {
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

        preferences.putValueInPreferencesIfEmpty(
                browserInstance.getBrowserName(), latestVersion);
        return latestVersion;
    }

    private String getLatestVersionFromProperties(
            BrowserInstance browserInstance) {
        String latestVersion = null;
        switch (browserInstance.getBrowserType()) {
        case FIREFOX:
            latestVersion = getConfig().getFirefoxLatestVersion();
            break;
        case OPERA:
            latestVersion = getConfig().getOperaLatestVersion();
            break;
        case CHROME:
        default:
            latestVersion = getConfig().getChromeLatestVersion();
            break;
        }
        return latestVersion;
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

    public Config getConfig() {
        return config;
    }

    public String getVersion() {
        return version;
    }

    public class BrowserConfig {
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

    public class Browser {
        String image;
        String port = getConfig().getSelenoidPort();
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

    public class Tmpfs {
        @SerializedName("/tmp")
        String tmp = "size=" + getConfig().getSelenoidTmpfsSize();
    }

}
