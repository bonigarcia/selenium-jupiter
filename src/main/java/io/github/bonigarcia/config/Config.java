/*
 * (C) Copyright 2018 Boni Garcia (http://bonigarcia.github.io/)
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
package io.github.bonigarcia.config;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import org.slf4j.Logger;

import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Configuration class.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.1.0
 */
public class Config {

    final Logger log = getLogger(lookup().lookupClass());

    ConfigKey<String> properties = new ConfigKey<>("sel.jup.properties",
            String.class, "selenium-jupiter.properties");
    ConfigKey<String> seleniumServerUrl = new ConfigKey<>(
            "sel.jup.selenium.server.url", String.class);
    ConfigKey<Boolean> vnc = new ConfigKey<>("sel.jup.vnc", Boolean.class);
    ConfigKey<String> vncScreenResolution = new ConfigKey<>(
            "sel.jup.vnc.screen.resolution", String.class);
    ConfigKey<Boolean> vncRedirectHtmlPage = new ConfigKey<>(
            "sel.jup.vnc.create.redirect.html.page", Boolean.class);
    ConfigKey<String> vncExport = new ConfigKey<>("sel.jup.vnc.export",
            String.class);
    ConfigKey<Boolean> recording = new ConfigKey<>("sel.jup.recording",
            Boolean.class);
    ConfigKey<String> recordingVideoScreenSize = new ConfigKey<>(
            "sel.jup.recording.video.screen.size", String.class);
    ConfigKey<Integer> recordingVideoFrameRate = new ConfigKey<>(
            "sel.jup.recording.video.frame.rate", Integer.class);
    ConfigKey<String> recordingImage = new ConfigKey<>(
            "sel.jup.recording.image", String.class);
    ConfigKey<String> outputFolder = new ConfigKey<>("sel.jup.output.folder",
            String.class);
    ConfigKey<String> screenshotAtTheEndOfTests = new ConfigKey<>(
            "sel.jup.screenshot.at.the.end.of.tests", String.class);
    ConfigKey<String> screenshotFormat = new ConfigKey<>(
            "sel.jup.screenshot.format", String.class);
    ConfigKey<Boolean> exceptionWhenNoDriver = new ConfigKey<>(
            "sel.jup.exception.when.no.driver", Boolean.class);
    ConfigKey<String> browserTemplateJsonFile = new ConfigKey<>(
            "sel.jup.browser.template.json.file", String.class);
    ConfigKey<String> browserTemplateJsonContent = new ConfigKey<>(
            "sel.jup.browser.template.json.content", String.class);
    ConfigKey<String> defaultBrowser = new ConfigKey<>(
            "sel.jup.default.browser", String.class);
    ConfigKey<String> defaultVersion = new ConfigKey<>(
            "sel.jup.default.version", String.class);
    ConfigKey<String> defaultBrowserFallback = new ConfigKey<>(
            "sel.jup.default.browser.fallback", String.class);
    ConfigKey<String> defaultBrowserFallbackVersion = new ConfigKey<>(
            "sel.jup.default.browser.fallback.version", String.class);
    ConfigKey<Boolean> browserListFromDockerHub = new ConfigKey<>(
            "sel.jup.browser.list.from.docker.hub", Boolean.class);
    ConfigKey<String> browserSessionTimeoutDuration = new ConfigKey<>(
            "sel.jup.browser.session.timeout.duration", String.class);
    ConfigKey<Boolean> browserListInParallel = new ConfigKey<>(
            "sel.jup.browser.list.in.parallel", Boolean.class);
    ConfigKey<String> selenoidImage = new ConfigKey<>("sel.jup.selenoid.image",
            String.class);
    ConfigKey<String> selenoidPort = new ConfigKey<>("sel.jup.selenoid.port",
            String.class);
    ConfigKey<String> selenoidVncPassword = new ConfigKey<>(
            "sel.jup.selenoid.vnc.password", String.class);
    ConfigKey<String> selenoidTmpfsSize = new ConfigKey<>(
            "sel.jup.selenoid.tmpfs.size", String.class);
    ConfigKey<String> novncImage = new ConfigKey<>("sel.jup.novnc.image",
            String.class);
    ConfigKey<String> novncPort = new ConfigKey<>("sel.jup.novnc.port",
            String.class);
    ConfigKey<String> chromeImageFormat = new ConfigKey<>(
            "sel.jup.chrome.image.format", String.class);
    ConfigKey<String> chromeFirstVersion = new ConfigKey<>(
            "sel.jup.chrome.first.version", String.class);
    ConfigKey<String> chromeLatestVersion = new ConfigKey<>(
            "sel.jup.chrome.latest.version", String.class);
    ConfigKey<String> chromePath = new ConfigKey<>("sel.jup.chrome.path",
            String.class);
    ConfigKey<String> chromeBetaImage = new ConfigKey<>(
            "sel.jup.chrome.beta.image", String.class);
    ConfigKey<String> chromeBetaPath = new ConfigKey<>(
            "sel.jup.chrome.beta.path", String.class);
    ConfigKey<String> chromeUnstableImage = new ConfigKey<>(
            "sel.jup.chrome.unstable.image", String.class);
    ConfigKey<String> chromeUnstablePath = new ConfigKey<>(
            "sel.jup.chrome.unstable.path", String.class);
    ConfigKey<String> firefoxImageFormat = new ConfigKey<>(
            "sel.jup.firefox.image.format", String.class);
    ConfigKey<String> firefoxFirstVersion = new ConfigKey<>(
            "sel.jup.firefox.first.version", String.class);
    ConfigKey<String> firefoxLatestVersion = new ConfigKey<>(
            "sel.jup.firefox.latest.version", String.class);
    ConfigKey<String> firefoxPath = new ConfigKey<>("sel.jup.firefox.path",
            String.class);
    ConfigKey<String> firefoxBetaImage = new ConfigKey<>(
            "sel.jup.firefox.beta.image", String.class);
    ConfigKey<String> firefoxBetaPath = new ConfigKey<>(
            "sel.jup.firefox.beta.path", String.class);
    ConfigKey<String> firefoxUnstableImage = new ConfigKey<>(
            "sel.jup.firefox.unstable.image", String.class);
    ConfigKey<String> firefoxUnstablePath = new ConfigKey<>(
            "sel.jup.firefox.unstable.path", String.class);
    ConfigKey<String> operaImageFormat = new ConfigKey<>(
            "sel.jup.opera.image.format", String.class);
    ConfigKey<String> operaFirstVersion = new ConfigKey<>(
            "sel.jup.opera.first.version", String.class);
    ConfigKey<String> operaLatestVersion = new ConfigKey<>(
            "sel.jup.opera.latest.version", String.class);
    ConfigKey<String> operaPath = new ConfigKey<>("sel.jup.opera.path",
            String.class);
    ConfigKey<Integer> dockerWaitTimeoutSec = new ConfigKey<>(
            "sel.jup.docker.wait.timeout.sec", Integer.class);
    ConfigKey<Integer> dockerPollTimeMs = new ConfigKey<>(
            "sel.jup.docker.poll.time.ms", Integer.class);
    ConfigKey<String> dockerDefaultSocket = new ConfigKey<>(
            "sel.jup.docker.default.socket", String.class);
    ConfigKey<String> dockerHubUrl = new ConfigKey<>("sel.jup.docker.hub.url",
            String.class);
    ConfigKey<String> dockerServerUrl = new ConfigKey<>(
            "sel.jup.docker.server.url", String.class);
    ConfigKey<Integer> dockerStopTimeoutSec = new ConfigKey<>(
            "sel.jup.docker.stop.timeout.sec", Integer.class);
    ConfigKey<String> dockerApiVersion = new ConfigKey<>(
            "sel.jup.docker.api.version", String.class);
    ConfigKey<String> dockerNetwork = new ConfigKey<>("sel.jup.docker.network",
            String.class);
    ConfigKey<String> dockerTimeZone = new ConfigKey<>(
            "sel.jup.docker.timezone", String.class);

    ConfigKey<String> androidImage = new ConfigKey<>("sel.jup.android.image",
            String.class);
    ConfigKey<String> androidNoVncPort = new ConfigKey<>(
            "sel.jup.android.novnc.port", String.class);
    ConfigKey<String> androidAppiumPort = new ConfigKey<>(
            "sel.jup.android.appium.port", String.class);
    ConfigKey<String> androidDeviceName = new ConfigKey<>(
            "sel.jup.android.device.name", String.class);
    ConfigKey<String> androidBrowserName = new ConfigKey<>(
            "sel.jup.android.browser.name", String.class);

    private <T> T resolve(ConfigKey<T> configKey) {
        String strValue = null;
        String name = configKey.getName();
        T tValue = configKey.getValue();
        Class<T> type = configKey.getType();

        strValue = System.getenv(name.toUpperCase().replace(".", "_"));
        if (strValue == null) {
            strValue = System.getProperty(name);
        }
        if (strValue == null && tValue != null) {
            return tValue;
        }
        if (strValue == null) {
            strValue = getProperty(name);
        }
        return parse(type, strValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T parse(Class<T> type, String strValue) {
        T output = null;
        if (type.equals(String.class)) {
            output = (T) strValue;
        } else if (type.equals(Integer.class)) {
            output = (T) Integer.valueOf(strValue);
        } else if (type.equals(Boolean.class)) {
            output = (T) Boolean.valueOf(strValue);
        } else {
            throw new SeleniumJupiterException(
                    "Type " + type.getTypeName() + " cannot be parsed");
        }
        return output;
    }

    private String getProperty(String key) {
        String value = null;
        Properties props = new Properties();
        try {
            InputStream inputStream = Config.class
                    .getResourceAsStream("/" + getProperties());
            props.load(inputStream);
            value = props.getProperty(key);
        } catch (Exception e) {
            throw new SeleniumJupiterException(e);
        } finally {
            if (value == null) {
                log.trace("Property key {} not found, using default value",
                        key);
                value = "";
            }
        }
        return value;
    }

    public void reset() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType() == ConfigKey.class) {
                try {
                    ((ConfigKey<?>) field.get(this)).reset();
                } catch (Exception e) {
                    log.warn("Exception reseting {}", field);
                }
            }
        }
    }

    // Getters and setters

    public String getProperties() {
        return resolve(properties);
    }

    public void setProperties(boolean properties) {
        this.properties.setValue(properties);
    }

    public String getSeleniumServerUrl() {
        return resolve(seleniumServerUrl);
    }

    public void setSeleniumServerUrl(String value) {
        this.seleniumServerUrl.setValue(value);
    }

    public boolean isVnc() {
        return resolve(vnc);
    }

    public void setVnc(boolean value) {
        this.vnc.setValue(value);
    }

    public String getVncScreenResolution() {
        return resolve(vncScreenResolution);
    }

    public void setVncScreenResolution(String value) {
        this.vncScreenResolution.setValue(value);
    }

    public boolean isVncRedirectHtmlPage() {
        return resolve(vncRedirectHtmlPage);
    }

    public void setVncRedirectHtmlPage(boolean value) {
        this.vncRedirectHtmlPage.setValue(value);
    }

    public String getVncExport() {
        return resolve(vncExport);
    }

    public void setVncExport(String value) {
        this.vncExport.setValue(value);
    }

    public boolean isRecording() {
        return resolve(recording);
    }

    public void setRecording(boolean value) {
        this.recording.setValue(value);
    }

    public String getRecordingVideoScreenSize() {
        return resolve(recordingVideoScreenSize);
    }

    public void setRecordingVideoScreenSize(String value) {
        this.recordingVideoScreenSize.setValue(value);
    }

    public int getRecordingVideoFrameRate() {
        return resolve(recordingVideoFrameRate);
    }

    public void setRecordingVideoFrameRate(int value) {
        this.recordingVideoFrameRate.setValue(value);
    }

    public String getRecordingImage() {
        return resolve(recordingImage);
    }

    public void setRecordingImage(String value) {
        this.recordingImage.setValue(value);
    }

    public String getOutputFolder() {
        return resolve(outputFolder);
    }

    public void setOutputFolder(String value) {
        this.outputFolder.setValue(value);
    }

    public String getScreenshotAtTheEndOfTests() {
        return resolve(screenshotAtTheEndOfTests);
    }

    public void setScreenshotAtTheEndOfTests(String value) {
        this.screenshotAtTheEndOfTests.setValue(value);
    }

    public String getScreenshotFormat() {
        return resolve(screenshotFormat);
    }

    public void setScreenshotFormat(String value) {
        this.screenshotFormat.setValue(value);
    }

    public boolean isExceptionWhenNoDriver() {
        return resolve(exceptionWhenNoDriver);
    }

    public void setExceptionWhenNoDriver(boolean value) {
        this.exceptionWhenNoDriver.setValue(value);
    }

    public String getBrowserTemplateJsonFile() {
        return resolve(browserTemplateJsonFile);
    }

    public void setBrowserTemplateJsonFile(String value) {
        this.browserTemplateJsonFile.setValue(value);
    }

    public String getBrowserTemplateJsonContent() {
        return resolve(browserTemplateJsonContent);
    }

    public void setBrowserTemplateJsonContent(String value) {
        this.browserTemplateJsonContent.setValue(value);
    }

    public String getDefaultBrowser() {
        return resolve(defaultBrowser);
    }

    public void setDefaultBrowser(String value) {
        this.defaultBrowser.setValue(value);
    }

    public String getDefaultVersion() {
        return resolve(defaultVersion);
    }

    public void setDefaultVersion(String value) {
        this.defaultVersion.setValue(value);
    }

    public String getDefaultBrowserFallback() {
        return resolve(defaultBrowserFallback);
    }

    public void setDefaultBrowserFallback(String value) {
        this.defaultBrowserFallback.setValue(value);
    }

    public String getDefaultBrowserFallbackVersion() {
        return resolve(defaultBrowserFallbackVersion);
    }

    public void setDefaultBrowserFallbackVersion(String value) {
        this.defaultBrowserFallbackVersion.setValue(value);
    }

    public boolean isBrowserListFromDockerHub() {
        return resolve(browserListFromDockerHub);
    }

    public void setBrowserListFromDockerHub(boolean value) {
        this.browserListFromDockerHub.setValue(value);
    }

    public String getBrowserSessionTimeoutDuration() {
        return resolve(browserSessionTimeoutDuration);
    }

    public void setBrowserSessionTimeoutDuration(String value) {
        this.browserSessionTimeoutDuration.setValue(value);
    }

    public Boolean isBrowserListInParallel() {
        return resolve(browserListInParallel);
    }

    public void setBrowserListInParallel(Boolean value) {
        this.browserListInParallel.setValue(value);
    }

    public String getSelenoidImage() {
        return resolve(selenoidImage);
    }

    public void setSelenoidImage(String value) {
        this.selenoidImage.setValue(value);
    }

    public String getSelenoidPort() {
        return resolve(selenoidPort);
    }

    public void setSelenoidPort(String value) {
        this.selenoidPort.setValue(value);
    }

    public String getSelenoidVncPassword() {
        return resolve(selenoidVncPassword);
    }

    public void setSelenoidVncPassword(String value) {
        this.selenoidVncPassword.setValue(value);
    }

    public String getSelenoidTmpfsSize() {
        return resolve(selenoidTmpfsSize);
    }

    public void setSelenoidTmpfsSize(String value) {
        this.selenoidTmpfsSize.setValue(value);
    }

    public String getNovncImage() {
        return resolve(novncImage);
    }

    public void setNovncImage(String value) {
        this.novncImage.setValue(value);
    }

    public String getNovncPort() {
        return resolve(novncPort);
    }

    public void setNovncPort(String value) {
        this.novncPort.setValue(value);
    }

    public String getChromeImageFormat() {
        return resolve(chromeImageFormat);
    }

    public void setChromeImageFormat(String value) {
        this.chromeImageFormat.setValue(value);
    }

    public String getChromeFirstVersion() {
        return resolve(chromeFirstVersion);
    }

    public void setChromeFirstVersion(String value) {
        this.chromeFirstVersion.setValue(value);
    }

    public String getChromeLatestVersion() {
        return resolve(chromeLatestVersion);
    }

    public void setChromeLatestVersion(String value) {
        this.chromeLatestVersion.setValue(value);
    }

    public String getChromePath() {
        return resolve(chromePath);
    }

    public void setChromePath(String value) {
        this.chromePath.setValue(value);
    }

    public String getChromeBetaImage() {
        return resolve(chromeBetaImage);
    }

    public void setChromeBetaImage(String value) {
        this.chromeBetaImage.setValue(value);
    }

    public String getChromeBetaPath() {
        return resolve(chromeBetaPath);
    }

    public void setChromeBetaPath(String value) {
        this.chromeBetaPath.setValue(value);
    }

    public String getChromeUnstableImage() {
        return resolve(chromeUnstableImage);
    }

    public void setChromeUnstableImage(String value) {
        this.chromeUnstableImage.setValue(value);
    }

    public String getChromeUnstablePath() {
        return resolve(chromeUnstablePath);
    }

    public void setChromeUnstablePath(String value) {
        this.chromeUnstablePath.setValue(value);
    }

    public String getFirefoxImageFormat() {
        return resolve(firefoxImageFormat);
    }

    public void setFirefoxImageFormat(String value) {
        this.firefoxImageFormat.setValue(value);
    }

    public String getFirefoxFirstVersion() {
        return resolve(firefoxFirstVersion);
    }

    public void setFirefoxFirstVersion(String value) {
        this.firefoxFirstVersion.setValue(value);
    }

    public String getFirefoxLatestVersion() {
        return resolve(firefoxLatestVersion);
    }

    public void setFirefoxLatestVersion(String value) {
        this.firefoxLatestVersion.setValue(value);
    }

    public String getFirefoxPath() {
        return resolve(firefoxPath);
    }

    public void setFirefoxPath(String value) {
        this.firefoxPath.setValue(value);
    }

    public String getFirefoxBetaImage() {
        return resolve(firefoxBetaImage);
    }

    public void setFirefoxBetaImage(String value) {
        this.firefoxBetaImage.setValue(value);
    }

    public String getFirefoxBetaPath() {
        return resolve(firefoxBetaPath);
    }

    public void setFirefoxBetaPath(String value) {
        this.firefoxBetaPath.setValue(value);
    }

    public String getFirefoxUnstableImage() {
        return resolve(firefoxUnstableImage);
    }

    public void setFirefoxUnstableImage(String value) {
        this.firefoxUnstableImage.setValue(value);
    }

    public String getFirefoxUnstablePath() {
        return resolve(firefoxUnstablePath);
    }

    public void setFirefoxUnstablePath(String value) {
        this.firefoxUnstablePath.setValue(value);
    }

    public String getOperaImageFormat() {
        return resolve(operaImageFormat);
    }

    public void setOperaImageFormat(String value) {
        this.operaImageFormat.setValue(value);
    }

    public String getOperaFirstVersion() {
        return resolve(operaFirstVersion);
    }

    public void setOperaFirstVersion(String value) {
        this.operaFirstVersion.setValue(value);
    }

    public String getOperaLatestVersion() {
        return resolve(operaLatestVersion);
    }

    public void setOperaLatestVersion(String value) {
        this.operaLatestVersion.setValue(value);
    }

    public String getOperaPath() {
        return resolve(operaPath);
    }

    public void setOperaPath(String value) {
        this.operaPath.setValue(value);
    }

    public int getDockerWaitTimeoutSec() {
        return resolve(dockerWaitTimeoutSec);
    }

    public void setDockerWaitTimeoutSec(int dockerWaitTimeoutSec) {
        this.dockerWaitTimeoutSec.setValue(dockerWaitTimeoutSec);
    }

    public int getDockerPollTimeMs() {
        return resolve(dockerPollTimeMs);
    }

    public void setDockerPollTimeMs(int dockerWaitTimeoutSec) {
        this.dockerPollTimeMs.setValue(dockerWaitTimeoutSec);
    }

    public String getDockerDefaultSocket() {
        return resolve(dockerDefaultSocket);
    }

    public void setDockerDefaultSocket(String value) {
        this.dockerDefaultSocket.setValue(value);
    }

    public String getDockerHubUrl() {
        return resolve(dockerHubUrl);
    }

    public void setDockerHubUrl(String dockerHubUrl) {
        this.dockerHubUrl.setValue(dockerHubUrl);
    }

    public String getDockerServerUrl() {
        return resolve(dockerServerUrl);
    }

    public void setDockerServerUrl(String dockerHubUrl) {
        this.dockerServerUrl.setValue(dockerHubUrl);
    }

    public int getDockerStopTimeoutSec() {
        return resolve(dockerStopTimeoutSec);
    }

    public void setDockerStopTimeoutSec(int dockerWaitTimeoutSec) {
        this.dockerStopTimeoutSec.setValue(dockerWaitTimeoutSec);
    }

    public String getDockerApiVersion() {
        return resolve(dockerApiVersion);
    }

    public void setDockerApiVersion(String value) {
        this.dockerApiVersion.setValue(value);
    }

    public String getDockerNetwork() {
        return resolve(dockerNetwork);
    }

    public void setDockerNetwork(String value) {
        this.dockerNetwork.setValue(value);
    }

    public String getDockerTimeZone() {
        return resolve(dockerTimeZone);
    }

    public void setDockerTimeZone(String value) {
        this.dockerTimeZone.setValue(value);
    }

    public String getAndroidImage() {
        return resolve(androidImage);
    }

    public void setAndroidImage(String value) {
        this.androidImage.setValue(value);
    }

    public String getAndroidNoVncPort() {
        return resolve(androidNoVncPort);
    }

    public void setAndroidNoVncPort(String value) {
        this.androidNoVncPort.setValue(value);
    }

    public String getAndroidAppiumPort() {
        return resolve(androidAppiumPort);
    }

    public void setAndroidAppiumPort(String value) {
        this.androidAppiumPort.setValue(value);
    }

    public String getAndroidDeviceName() {
        return resolve(androidDeviceName);
    }

    public void setAndroidDeviceName(String value) {
        this.androidDeviceName.setValue(value);
    }

    public String getAndroidBrowserName() {
        return resolve(androidBrowserName);
    }

    public void setAndroidBrowserName(String value) {
        this.androidBrowserName.setValue(value);
    }

    // Custom values

    public void useSurefireOutputFolder() {
        this.outputFolder.setValue("surefire-reports");
    }

    public void enableScreenshotAtTheEndOfTests() {
        this.screenshotAtTheEndOfTests.setValue("true");
    }

    public void disableScreenshotAtTheEndOfTests() {
        this.screenshotAtTheEndOfTests.setValue("false");
    }

    public void takeScreenshotAsBase64() {
        this.screenshotFormat.setValue("base64");
    }

    public void takeScreenshotAsPng() {
        this.screenshotFormat.setValue("png");
    }

    public void takeScreenshotAsBase64AndPng() {
        this.screenshotFormat.setValue("base64andpng");
    }

    public io.github.bonigarcia.wdm.Config wdm() {
        return WebDriverManager.config();
    }

}
