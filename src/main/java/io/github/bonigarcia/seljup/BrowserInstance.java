/*
 * (C) Copyright 2019 Boni Garcia (http://bonigarcia.github.io/)
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

import static java.lang.Integer.parseInt;
import static java.lang.invoke.MethodHandles.lookup;
import static org.openqa.selenium.Platform.ANY;
import static org.slf4j.LoggerFactory.getLogger;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;

import io.github.bonigarcia.seljup.config.Config;
import io.github.bonigarcia.seljup.handler.ChromeDriverHandler;
import io.github.bonigarcia.seljup.handler.DriverHandler;
import io.github.bonigarcia.seljup.handler.FirefoxDriverHandler;
import io.github.bonigarcia.seljup.handler.OperaDriverHandler;

/**
 * Browser instance.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 3.0.0
 */
public class BrowserInstance {

    final Logger log = getLogger(lookup().lookupClass());

    Config config;
    BrowserType browserType;
    String dockerImage;
    String path;
    DriverHandler driverHandler;
    String optionsKey;
    DesiredCapabilities capabilities;

    public BrowserInstance(Config config, BrowserType browserType) {
        this.config = config;
        this.browserType = browserType;

        switch (browserType) {
        case ANDROID:
            driverHandler = new ChromeDriverHandler(config);
            optionsKey = ChromeOptions.CAPABILITY;
            capabilities = new DesiredCapabilities();
            break;
        case FIREFOX:
            dockerImage = config.getFirefoxImageFormat();
            path = config.getFirefoxPath();
            driverHandler = new FirefoxDriverHandler(config);
            optionsKey = FirefoxOptions.FIREFOX_OPTIONS;
            capabilities = new DesiredCapabilities("firefox", "", ANY);
            break;
        case OPERA:
            dockerImage = config.getOperaImageFormat();
            path = config.getOperaPath();
            driverHandler = new OperaDriverHandler(config);
            optionsKey = OperaOptions.CAPABILITY;
            capabilities = new DesiredCapabilities("operablink", "", ANY);
            break;
        case CHROME:
        default:
            dockerImage = config.getChromeImageFormat();
            path = config.getChromePath();
            driverHandler = new ChromeDriverHandler(config);
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

    public BrowserType getBrowserType() {
        return browserType;
    }

    public String getBrowserName() {
        return browserType.name();
    }

}
