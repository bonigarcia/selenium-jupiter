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

import static io.github.bonigarcia.seljup.BrowserType.ANDROID;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.google.gson.GsonBuilder;

import io.github.bonigarcia.seljup.DockerBrowserConfig.Browser;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Utilities related with Selenoid.
 *
 * @author Boni Garcia
 * @since 1.2.0
 */
public class SelenoidConfig {

    final Logger log = getLogger(lookup().lookupClass());

    DockerBrowserConfig browsers;
    Config config;

    public SelenoidConfig() {
        // Default constructor
    }

    public SelenoidConfig(Config config, BrowserInstance browserInstance,
            String version) {
        this.config = config;
        if (browserInstance.getBrowserType() != ANDROID) {
            browsers = new DockerBrowserConfig(getDockerEnvs(), getConfig(),
                    browserInstance, version);
        }
    }

    public String getBrowsersJsonAsString() {
        return new GsonBuilder().disableHtmlEscaping().create()
                .toJson(browsers);
    }

    public String getImageVersion(BrowserType browser, String version) {
        Map<String, Browser> versions = browsers.getBrowserConfig(browser)
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
        return browsers.getBrowserConfig(browser).getVersions()
                .get(getImageVersion(browser, version)).getImage();
    }

    public String getLatestImage(BrowserInstance browserInstance) {
        return format(browserInstance.getDockerImage(),
                getDefaultBrowser(browserInstance.getBrowserType()));
    }

    public String getDefaultBrowser(BrowserType browser) {
        return browsers.getBrowserConfig(browser).getDefaultBrowser();
    }

    public List<String> getDockerEnvs() {
        List<String> envs = new ArrayList<>();
        envs.add("DOCKER_API_VERSION=" + getConfig().getDockerApiVersion());
        envs.add("TZ=" + getConfig().getDockerTimeZone());
        envs.add("LANG=" + getConfig().getDockerLang());
        if (getConfig().isVnc()) {
            envs.add("ENABLE_WINDOW_MANAGER=true");
        }
        return envs;
    }

    public Config getConfig() {
        return config;
    }

    public DockerBrowserConfig getDockerBrowserConfig() {
        return browsers;
    }

}
