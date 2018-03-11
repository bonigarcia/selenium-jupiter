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

import com.github.drapostolos.typeparser.TypeParser;

import io.github.bonigarcia.SeleniumJupiterException;

/**
 * Configuration class.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.1.0
 */
public class Config {

    final Logger log = getLogger(lookup().lookupClass());

    TypeParser parser = TypeParser.newBuilder().build();

    ConfigKey<String> properties = new ConfigKey<>("sel.jup.properties",
            String.class, "selenium-jupiter.properties");
    ConfigKey<Boolean> vnc = new ConfigKey<>("sel.jup.vnc", Boolean.class);
    ConfigKey<String> vncScreenResolution = new ConfigKey<>(
            "sel.jup.vnc.screen.resolution", String.class);
    ConfigKey<Boolean> vncRedirectHtmlPage = new ConfigKey<>(
            "sel.jup.vnc.create.redirect.html.page", Boolean.class);
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
    ConfigKey<Boolean> browserListFromDockerHub = new ConfigKey<Boolean>(
            "sel.jup.browser.list.from.docker.hub", Boolean.class);
    ConfigKey<String> browserSessionTimeoutDuration = new ConfigKey<>(
            "sel.jup.browser.session.timeout.duration", String.class);
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
    ConfigKey<String> firefoxImageFormat = new ConfigKey<>(
            "sel.jup.firefox.image.format", String.class);
    ConfigKey<String> firefoxFirstVersion = new ConfigKey<>(
            "sel.jup.firefox.first.version", String.class);
    ConfigKey<String> firefoxLatestVersion = new ConfigKey<>(
            "sel.jup.firefox.latest.version", String.class);
    ConfigKey<String> firefoxPath = new ConfigKey<>("sel.jup.firefox.path",
            String.class);
    ConfigKey<String> operaImageFormat = new ConfigKey<>(
            "sel.jup.opera.image.format", String.class);
    ConfigKey<String> operaFirstVersion = new ConfigKey<>(
            "sel.jup.opera.first.version", String.class);
    ConfigKey<String> operaLatestVersion = new ConfigKey<>(
            "sel.jup.opera.latest.version", String.class);
    ConfigKey<String> operaPath = new ConfigKey<>("sel.jup.opera.path",
            String.class);
    ConfigKey<Integer> dockerWaitTimeoutSec = new ConfigKey<Integer>(
            "sel.jup.docker.wait.timeout.sec", Integer.class);
    ConfigKey<Integer> dockerPollTimeMs = new ConfigKey<Integer>(
            "sel.jup.docker.poll.time.ms", Integer.class);
    ConfigKey<String> dockerDefaultSocket = new ConfigKey<>(
            "sel.jup.docker.default.socket", String.class);
    ConfigKey<String> dockerDefaultHost = new ConfigKey<>(
            "sel.jup.docker.default.host", String.class);
    ConfigKey<String> dockerHubUrl = new ConfigKey<>("sel.jup.docker.hub.url",
            String.class);
    ConfigKey<String> dockerServerUrl = new ConfigKey<>(
            "sel.jup.docker.server.url", String.class);
    ConfigKey<Integer> dockerStopTimeoutSec = new ConfigKey<Integer>(
            "sel.jup.docker.stop.timeout.sec", Integer.class);
    ConfigKey<String> dockerApiVersion = new ConfigKey<>(
            "sel.jup.docker.api.version", String.class);
    ConfigKey<String> dockerNetwork = new ConfigKey<>("sel.jup.docker.network",
            String.class);

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
        return parser.parse(strValue, type);
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
        return (String) resolve(properties);
    }

    public void setProperties(boolean properties) {
        this.properties.setValue(properties);
    }

    public boolean isVnc() {
        return (boolean) resolve(vnc);
    }

    public void setVnc(boolean value) {
        this.vnc.setValue(value);
    }

    public String getVncScreenResolution() {
        return (String) resolve(vncScreenResolution);
    }

    public void setVncScreenResolution(String value) {
        this.vncScreenResolution.setValue(value);
    }

    public boolean isVncRedirectHtmlPage() {
        return (boolean) resolve(vncRedirectHtmlPage);
    }

    public void setVncRedirectHtmlPage(boolean value) {
        this.vncRedirectHtmlPage.setValue(value);
    }

    public boolean isRecording() {
        return (boolean) resolve(recording);
    }

    public void setRecording(boolean value) {
        this.recording.setValue(value);
    }

    public String getRecordingVideoScreenSize() {
        return (String) resolve(recordingVideoScreenSize);
    }

    public void setRecordingVideoScreenSize(String value) {
        this.recordingVideoScreenSize.setValue(value);
    }

    public int getRecordingVideoFrameRate() {
        return (int) resolve(recordingVideoFrameRate);
    }

    public void setRecordingVideoFrameRate(int value) {
        this.recordingVideoFrameRate.setValue(value);
    }

    public String getRecordingImage() {
        return (String) resolve(recordingImage);
    }

    public void setRecordingImage(String value) {
        this.recordingImage.setValue(value);
    }

    public String getOutputFolder() {
        return (String) resolve(outputFolder);
    }

    public void setOutputFolder(String value) {
        this.outputFolder.setValue(value);
    }

    public String getScreenshotAtTheEndOfTests() {
        return (String) resolve(screenshotAtTheEndOfTests);
    }

    public void setScreenshotAtTheEndOfTests(String value) {
        this.screenshotAtTheEndOfTests.setValue(value);
    }

    public String getScreenshotFormat() {
        return (String) resolve(screenshotFormat);
    }

    public void setScreenshotFormat(String value) {
        this.screenshotFormat.setValue(value);
    }

    public boolean isExceptionWhenNoDriver() {
        return (boolean) resolve(exceptionWhenNoDriver);
    }

    public void setExceptionWhenNoDriver(boolean value) {
        this.exceptionWhenNoDriver.setValue(value);
    }

    public String getBrowserTemplateJsonFile() {
        return (String) resolve(browserTemplateJsonFile);
    }

    public void setBrowserTemplateJsonFile(String value) {
        this.browserTemplateJsonFile.setValue(value);
    }

    public String getBrowserTemplateJsonContent() {
        return (String) resolve(browserTemplateJsonContent);
    }

    public void setBrowserTemplateJsonContent(String value) {
        this.browserTemplateJsonContent.setValue(value);
    }

    public String getDefaultBrowser() {
        return (String) resolve(defaultBrowser);
    }

    public void setDefaultBrowser(String value) {
        this.defaultBrowser.setValue(value);
    }

    public String getDefaultVersion() {
        return (String) resolve(defaultVersion);
    }

    public void setDefaultVersion(String value) {
        this.defaultVersion.setValue(value);
    }

    public String getDefaultBrowserFallback() {
        return (String) resolve(defaultBrowserFallback);
    }

    public void setDefaultBrowserFallback(String value) {
        this.defaultBrowserFallback.setValue(value);
    }

    public String getDefaultBrowserFallbackVersion() {
        return (String) resolve(defaultBrowserFallbackVersion);
    }

    public void setDefaultBrowserFallbackVersion(String value) {
        this.defaultBrowserFallbackVersion.setValue(value);
    }

    public boolean isBrowserListFromDockerHub() {
        return (boolean) resolve(browserListFromDockerHub);
    }

    public void setBrowserListFromDockerHub(boolean value) {
        this.browserListFromDockerHub.setValue(value);
    }

    public String getBrowserSessionTimeoutDuration() {
        return (String) resolve(browserSessionTimeoutDuration);
    }

    public void setBrowserSessionTimeoutDuration(String value) {
        this.browserSessionTimeoutDuration.setValue(value);
    }

    public String getSelenoidImage() {
        return (String) resolve(selenoidImage);
    }

    public void setSelenoidImage(String value) {
        this.selenoidImage.setValue(value);
    }

    public String getSelenoidPort() {
        return (String) resolve(selenoidPort);
    }

    public void setSelenoidPort(String value) {
        this.selenoidPort.setValue(value);
    }

    public String getSelenoidVncPassword() {
        return (String) resolve(selenoidVncPassword);
    }

    public void setSelenoidVncPassword(String value) {
        this.selenoidVncPassword.setValue(value);
    }

    public String getSelenoidTmpfsSize() {
        return (String) resolve(selenoidTmpfsSize);
    }

    public void setSelenoidTmpfsSize(String value) {
        this.selenoidTmpfsSize.setValue(value);
    }

    public String getNovncImage() {
        return (String) resolve(novncImage);
    }

    public void setNovncImage(String value) {
        this.novncImage.setValue(value);
    }

    public String getNovncPort() {
        return (String) resolve(novncPort);
    }

    public void setNovncPort(String value) {
        this.novncPort.setValue(value);
    }

    public String getChromeImageFormat() {
        return (String) resolve(chromeImageFormat);
    }

    public void setChromeImageFormat(String value) {
        this.chromeImageFormat.setValue(value);
    }

    public String getChromeFirstVersion() {
        return (String) resolve(chromeFirstVersion);
    }

    public void setChromeFirstVersion(String value) {
        this.chromeFirstVersion.setValue(value);
    }

    public String getChromeLatestVersion() {
        return (String) resolve(chromeLatestVersion);
    }

    public void setChromeLatestVersion(String value) {
        this.chromeLatestVersion.setValue(value);
    }

    public String getChromePath() {
        return (String) resolve(chromePath);
    }

    public void setChromePath(String value) {
        this.chromePath.setValue(value);
    }

    public String getFirefoxImageFormat() {
        return (String) resolve(firefoxImageFormat);
    }

    public void setFirefoxImageFormat(String value) {
        this.firefoxImageFormat.setValue(value);
    }

    public String getFirefoxFirstVersion() {
        return (String) resolve(firefoxFirstVersion);
    }

    public void setFirefoxFirstVersion(String value) {
        this.firefoxFirstVersion.setValue(value);
    }

    public String getFirefoxLatestVersion() {
        return (String) resolve(firefoxLatestVersion);
    }

    public void setFirefoxLatestVersion(String value) {
        this.firefoxLatestVersion.setValue(value);
    }

    public String getFirefoxPath() {
        return (String) resolve(firefoxPath);
    }

    public void setFirefoxPath(String value) {
        this.firefoxPath.setValue(value);
    }

    public String getOperaImageFormat() {
        return (String) resolve(operaImageFormat);
    }

    public void setOperaImageFormat(String value) {
        this.operaImageFormat.setValue(value);
    }

    public String getOperaFirstVersion() {
        return (String) resolve(operaFirstVersion);
    }

    public void setOperaFirstVersion(String value) {
        this.operaFirstVersion.setValue(value);
    }

    public String getOperaLatestVersion() {
        return (String) resolve(operaLatestVersion);
    }

    public void setOperaLatestVersion(String value) {
        this.operaLatestVersion.setValue(value);
    }

    public String getOperaPath() {
        return (String) resolve(operaPath);
    }

    public void setOperaPath(String value) {
        this.operaPath.setValue(value);
    }

    public int getDockerWaitTimeoutSec() {
        return (int) resolve(dockerWaitTimeoutSec);
    }

    public void setDockerWaitTimeoutSec(int dockerWaitTimeoutSec) {
        this.dockerWaitTimeoutSec.setValue(dockerWaitTimeoutSec);
    }

    public int getDockerPollTimeMs() {
        return (int) resolve(dockerPollTimeMs);
    }

    public void setDockerPollTimeMs(int dockerWaitTimeoutSec) {
        this.dockerPollTimeMs.setValue(dockerWaitTimeoutSec);
    }

    public String getDockerDefaultSocket() {
        return (String) resolve(dockerDefaultSocket);
    }

    public void setDockerDefaultSocket(String value) {
        this.dockerDefaultSocket.setValue(value);
    }

    public String getDockerDefaultHost() {
        return (String) resolve(dockerDefaultHost);
    }

    public void setDockerDefaultHost(String value) {
        this.dockerDefaultHost.setValue(value);
    }

    public String getDockerHubUrl() {
        return (String) resolve(dockerHubUrl);
    }

    public void setDockerHubUrl(String dockerHubUrl) {
        this.dockerHubUrl.setValue(dockerHubUrl);
    }

    public String getDockerServerUrl() {
        return (String) resolve(dockerServerUrl);
    }

    public void setDockerServerUrl(String dockerHubUrl) {
        this.dockerServerUrl.setValue(dockerHubUrl);
    }

    public int getDockerStopTimeoutSec() {
        return (int) resolve(dockerStopTimeoutSec);
    }

    public void setDockerStopTimeoutSec(int dockerWaitTimeoutSec) {
        this.dockerStopTimeoutSec.setValue(dockerWaitTimeoutSec);
    }

    public String getDockerApiVersion() {
        return (String) resolve(dockerApiVersion);
    }

    public void setDockerApiVersion(String value) {
        this.dockerApiVersion.setValue(value);
    }

    public String getDockerNetwork() {
        return (String) resolve(dockerNetwork);
    }

    public void setDockerNetwork(String value) {
        this.dockerNetwork.setValue(value);
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

    public void whenFailureScreenshotAtTheEndOfTests() {
        this.screenshotAtTheEndOfTests.setValue("whenfailure");
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

}
