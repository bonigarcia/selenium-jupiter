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

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import org.slf4j.Logger;

import com.google.gson.Gson;

import io.github.bonigarcia.DockerBrowserConfig.Browser;

/**
 * Utilities related with Selenoid.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class SelenoidConfig {

    final Logger log = getLogger(lookup().lookupClass());

    DockerBrowserConfig browsers;

    public SelenoidConfig() {
        browsers = new DockerBrowserConfig();
    }

    public String getBrowsersJsonAsString() {
        return new Gson().toJson(browsers);
    }

    public String getVersionFromLabel(BrowserType browser, String label) {
        int beforeVersion = Integer.parseInt(label.replace("latest-", ""));
        String latestVersion = getLatestImage(browser);
        String previousVersion = getPreviousVersion(beforeVersion,
                latestVersion);
        log.debug("Version {} for {} (latest version {}) = {}", label, browser,
                latestVersion, previousVersion);
        return previousVersion;
    }

    public String getImageVersion(BrowserType browser, String version) {
        Map<String, Browser> versions = browsers.getBrowser(browser)
                .getVersions();

        if (versions.containsKey(version)) {
            return version;
        }

        for (String v : versions.keySet()) {
            if (v.startsWith(version)) {
                return v;
            }
        }

        throw new SeleniumJupiterException(
                "Version " + version + " is not valid for Chrome");

    }

    public String getImageFromVersion(BrowserType browser, String version) {
        return browsers.getBrowser(browser).getVersions()
                .get(getImageVersion(browser, version)).getImage();
    }

    public String getLatestImage(BrowserType browser) {
        return format(browser.getDockerImage(), getDefaultBrowser(browser));
    }

    public String getDefaultBrowser(BrowserType browser) {
        return browsers.getBrowser(browser).getDefaultBrowser();
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
}
