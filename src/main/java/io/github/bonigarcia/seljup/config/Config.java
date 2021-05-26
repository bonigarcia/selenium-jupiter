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
package io.github.bonigarcia.seljup.config;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;

import io.github.bonigarcia.seljup.SeleniumJupiterException;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Configuration class.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.1.0
 */
public class Config {

    final Logger log = getLogger(lookup().lookupClass());

    static final String HOME = "~";

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
    ConfigKey<Boolean> recordingWhenFailure = new ConfigKey<>(
            "sel.jup.recording.when.failure", Boolean.class);
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
    ConfigKey<String> edgeImage = new ConfigKey<>("sel.jup.edge.image",
            String.class);
    ConfigKey<String> edgePath = new ConfigKey<>("sel.jup.edge.path",
            String.class);
    ConfigKey<String> edgeLatestVersion = new ConfigKey<>(
            "sel.jup.edge.latest.version", String.class);
    ConfigKey<String> iExplorerImage = new ConfigKey<>(
            "sel.jup.iexplorer.image", String.class);
    ConfigKey<String> iExplorerPath = new ConfigKey<>("sel.jup.iexplorer.path",
            String.class);
    ConfigKey<String> iExplorerLatestVersion = new ConfigKey<>(
            "sel.jup.iexplorer.latest.version", String.class);
    ConfigKey<String> operaImageFormat = new ConfigKey<>(
            "sel.jup.opera.image.format", String.class);
    ConfigKey<String> operaFirstVersion = new ConfigKey<>(
            "sel.jup.opera.first.version", String.class);
    ConfigKey<String> operaLatestVersion = new ConfigKey<>(
            "sel.jup.opera.latest.version", String.class);
    ConfigKey<String> operaPath = new ConfigKey<>("sel.jup.opera.path",
            String.class);
    ConfigKey<String> operaBinaryPathLinux = new ConfigKey<>(
            "sel.jup.opera.binary.path.linux", String.class);
    ConfigKey<String> operaBinaryPathWin = new ConfigKey<>(
            "sel.jup.opera.binary.path.win", String.class);
    ConfigKey<String> operaBinaryPathMac = new ConfigKey<>(
            "sel.jup.opera.binary.path.mac", String.class);
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
    ConfigKey<String> dockerHost = new ConfigKey<>("sel.jup.docker.host",
            String.class);
    ConfigKey<String> dockerNetwork = new ConfigKey<>("sel.jup.docker.network",
            String.class);
    ConfigKey<String> dockerTimeZone = new ConfigKey<>(
            "sel.jup.docker.timezone", String.class);
    ConfigKey<String> dockerLang = new ConfigKey<>("sel.jup.docker.lang",
            String.class);
    ConfigKey<String> dockerStartupTimeoutDuration = new ConfigKey<>(
            "sel.jup.docker.startup.timeout.duration", String.class);
    ConfigKey<String> dockerCache = new ConfigKey<>("sel.jup.docker.cache",
            String.class);
    ConfigKey<Boolean> dockerAvoidCache = new ConfigKey<>(
            "sel.jup.docker.avoid.cache", Boolean.class);

    ConfigKey<String> androidDefaultVersion = new ConfigKey<>(
            "sel.jup.android.default.version", String.class);
    ConfigKey<String> androidImage501 = new ConfigKey<>(
            "sel.jup.android.image.5.0.1", String.class);
    ConfigKey<String> androidImage511 = new ConfigKey<>(
            "sel.jup.android.image.5.1.1", String.class);
    ConfigKey<String> androidImage60 = new ConfigKey<>(
            "sel.jup.android.image.6.0", String.class);
    ConfigKey<String> androidImage701 = new ConfigKey<>(
            "sel.jup.android.image.7.0.1", String.class);
    ConfigKey<String> androidImage711 = new ConfigKey<>(
            "sel.jup.android.image.7.1.1", String.class);
    ConfigKey<String> androidImage80 = new ConfigKey<>(
            "sel.jup.android.image.8.0", String.class);
    ConfigKey<String> androidImage81 = new ConfigKey<>(
            "sel.jup.android.image.8.1", String.class);
    ConfigKey<String> androidImage90 = new ConfigKey<>(
            "sel.jup.android.image.9.0", String.class);
    ConfigKey<String> androidImageGenymotion = new ConfigKey<>(
            "sel.jup.android.image.genymotion", String.class);
    ConfigKey<String> androidGenymotionUser = new ConfigKey<>(
            "sel.jup.android.genymotion.user", String.class);
    ConfigKey<String> androidGenymotionPassword = new ConfigKey<>(
            "sel.jup.android.genymotion.password", String.class);
    ConfigKey<String> androidGenymotionLicense = new ConfigKey<>(
            "sel.jup.android.genymotion.license", String.class);
    ConfigKey<String> androidGenymotionTemplate = new ConfigKey<>(
            "sel.jup.android.genymotion.template", String.class);
    ConfigKey<String> androidGenymotionDeviceName = new ConfigKey<>(
            "sel.jup.android.genymotion.device.name", String.class);
    ConfigKey<String> androidGenymotionAndroidVersion = new ConfigKey<>(
            "sel.jup.android.genymotion.android.version", String.class);
    ConfigKey<String> androidGenymotionAndroidApi = new ConfigKey<>(
            "sel.jup.android.genymotion.android.api", String.class);
    ConfigKey<String> androidGenymotionScreenSize = new ConfigKey<>(
            "sel.jup.android.genymotion.screen.size", String.class);
    ConfigKey<String> androidGenymotionChromedriver = new ConfigKey<>(
            "sel.jup.android.genymotion.chromedriver", String.class);
    ConfigKey<String> androidNoVncPort = new ConfigKey<>(
            "sel.jup.android.novnc.port", String.class);
    ConfigKey<String> androidAppiumPort = new ConfigKey<>(
            "sel.jup.android.appium.port", String.class);
    ConfigKey<String> androidDeviceName = new ConfigKey<>(
            "sel.jup.android.device.name", String.class);
    ConfigKey<Integer> androidDeviceTimeoutSec = new ConfigKey<>(
            "sel.jup.android.device.timeout.sec", Integer.class);
    ConfigKey<Integer> androidDeviceStartupTimeoutSec = new ConfigKey<>(
            "sel.jup.android.device.startup.timeout.sec", Integer.class);
    ConfigKey<Integer> androidAppiumPingPeriodSec = new ConfigKey<>(
            "sel.jup.android.appium.ping.period.sec", Integer.class);
    ConfigKey<Boolean> androidLogging = new ConfigKey<>(
            "sel.jup.android.logging", Boolean.class);
    ConfigKey<String> androidLogsFolder = new ConfigKey<>(
            "sel.jup.android.logs.folder", String.class);
    ConfigKey<String> androidAppiumLogLevel = new ConfigKey<>(
            "sel.jup.android.appium.loglevel", String.class);
    ConfigKey<String> androidAppiumLogFile = new ConfigKey<>(
            "sel.jup.android.appium.logfile", String.class);
    ConfigKey<String> androidScreenWidth = new ConfigKey<>(
            "sel.jup.android.screen.width", String.class);
    ConfigKey<String> androidScreenHeigth = new ConfigKey<>(
            "sel.jup.android.screen.height", String.class);
    ConfigKey<String> androidScreenDepth = new ConfigKey<>(
            "sel.jup.android.screen.depth", String.class);

    ConfigKey<Integer> serverPort = new ConfigKey<>("sel.jup.server.port",
            Integer.class);
    ConfigKey<String> serverPath = new ConfigKey<>("sel.jup.server.path",
            String.class);
    ConfigKey<Integer> serverTimeoutSec = new ConfigKey<>(
            "sel.jup.server.timeout.sec", Integer.class);

    ConfigKey<Integer> remoteWebdriverWaitTimeoutSec = new ConfigKey<>(
            "sel.jup.remote.webdriver.wait.timeout.sec", Integer.class);
    ConfigKey<Integer> remoteWebdriverPollTimeSec = new ConfigKey<>(
            "sel.jup.remote.webdriver.poll.time.sec", Integer.class);
    ConfigKey<Integer> ttlSec = new ConfigKey<>("sel.jup.ttl.sec",
            Integer.class);
    ConfigKey<Boolean> useDockerCache = new ConfigKey<>(
            "sel.jup.wdm.use.docker.cache", Boolean.class);
    ConfigKey<String> cachePath = new ConfigKey<>("sel.jup.cache.path",
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

    public void setProperties(String properties) {
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

    public boolean isRecordingWhenFailure() {
        return resolve(recordingWhenFailure);
    }

    public void setRecordingWhenFailure(boolean value) {
        this.recordingWhenFailure.setValue(value);
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

    public String getEdgeImage() {
        return resolve(edgeImage);
    }

    public void setEdgeImage(String value) {
        this.edgeImage.setValue(value);
    }

    public String getEdgePath() {
        return resolve(edgePath);
    }

    public void setEdgePath(String value) {
        this.edgePath.setValue(value);
    }

    public String getEdgeLatestVersion() {
        return resolve(edgeLatestVersion);
    }

    public void setEdgeLatestVersion(String value) {
        this.edgeLatestVersion.setValue(value);
    }

    public String getIExplorerImage() {
        return resolve(iExplorerImage);
    }

    public void setIExplorerImage(String value) {
        this.iExplorerImage.setValue(value);
    }

    public String getIExplorerPath() {
        return resolve(iExplorerPath);
    }

    public void setIExplorerLatestVersion(String value) {
        this.iExplorerLatestVersion.setValue(value);
    }

    public String getIExplorerLatestVersion() {
        return resolve(iExplorerLatestVersion);
    }

    public void setIExplorerPath(String value) {
        this.iExplorerPath.setValue(value);
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

    public String getOperaBinaryPathLinux() {
        return resolve(operaBinaryPathLinux);
    }

    public void setOperaBinaryPathLinux(String value) {
        this.operaBinaryPathLinux.setValue(value);
    }

    public String getOperaBinaryPathWin() {
        return resolve(operaBinaryPathWin);
    }

    public void setOperaBinaryPathWin(String value) {
        this.operaBinaryPathWin.setValue(value);
    }

    public String getOperaBinaryPathMac() {
        return resolve(operaBinaryPathMac);
    }

    public void setOperaBinaryPathMac(String value) {
        this.operaBinaryPathMac.setValue(value);
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

    public String getDockerHost() {
        return resolve(dockerHost);
    }

    public void setDockerHost(String value) {
        this.dockerHost.setValue(value);
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

    public String getDockerLang() {
        return resolve(dockerLang);
    }

    public void setDockerLang(String value) {
        this.dockerLang.setValue(value);
    }

    public String getDockerStartupTimeoutDuration() {
        return resolve(dockerStartupTimeoutDuration);
    }

    public void setDockerStartupTimeoutDuration(String value) {
        this.dockerStartupTimeoutDuration.setValue(value);
    }

    public String getDockerCache() {
        return resolve(dockerCache);
    }

    public void setDockerCache(String value) {
        this.dockerCache.setValue(value);
    }

    public boolean isDockerAvoidCache() {
        return resolve(dockerAvoidCache);
    }

    public void setDockerAvoidCaceh(boolean value) {
        this.dockerAvoidCache.setValue(value);
    }

    public String getAndroidDefaultVersion() {
        return resolve(androidDefaultVersion);
    }

    public void setAndroidDefaultVersion(String value) {
        this.androidDefaultVersion.setValue(value);
    }

    public String getAndroidImage501() {
        return resolve(androidImage501);
    }

    public void setAndroidImage501(String value) {
        this.androidImage501.setValue(value);
    }

    public String getAndroidImage511() {
        return resolve(androidImage511);
    }

    public void setAndroidImage511(String value) {
        this.androidImage511.setValue(value);
    }

    public String getAndroidImage60() {
        return resolve(androidImage60);
    }

    public void setAndroidImage60(String value) {
        this.androidImage60.setValue(value);
    }

    public String getAndroidImage701() {
        return resolve(androidImage701);
    }

    public void setAndroidImage701(String value) {
        this.androidImage701.setValue(value);
    }

    public String getAndroidImage711() {
        return resolve(androidImage711);
    }

    public void setAndroidImage711(String value) {
        this.androidImage711.setValue(value);
    }

    public String getAndroidImage80() {
        return resolve(androidImage80);
    }

    public void setAndroidImage80(String value) {
        this.androidImage80.setValue(value);
    }

    public String getAndroidImage81() {
        return resolve(androidImage81);
    }

    public void setAndroidImage81(String value) {
        this.androidImage81.setValue(value);
    }

    public String getAndroidImage90() {
        return resolve(androidImage90);
    }

    public void setAndroidImage90(String value) {
        this.androidImage90.setValue(value);
    }

    public String getAndroidImageGenymotion() {
        return resolve(androidImageGenymotion);
    }

    public void setAndroidImageGenymotion(String value) {
        this.androidImageGenymotion.setValue(value);
    }

    public String getAndroidGenymotionUser() {
        return resolve(androidGenymotionUser);
    }

    public void setAndroidGenymotionUser(String value) {
        this.androidGenymotionUser.setValue(value);
    }

    public String getAndroidGenymotionPassword() {
        return resolve(androidGenymotionPassword);
    }

    public void setAndroidGenymotionPassword(String value) {
        this.androidGenymotionPassword.setValue(value);
    }

    public String getAndroidGenymotionLicense() {
        return resolve(androidGenymotionLicense);
    }

    public void setAndroidGenymotionLicense(String value) {
        this.androidGenymotionLicense.setValue(value);
    }

    public String getAndroidGenymotionTemplate() {
        return resolve(androidGenymotionTemplate);
    }

    public void setAndroidGenymotionTemplate(String value) {
        this.androidGenymotionTemplate.setValue(value);
    }

    public String getAndroidGenymotionDeviceName() {
        return resolve(androidGenymotionDeviceName);
    }

    public void setAndroidGenymotionDeviceName(String value) {
        this.androidGenymotionDeviceName.setValue(value);
    }

    public String getAndroidGenymotionAndroidVersion() {
        return resolve(androidGenymotionAndroidVersion);
    }

    public void setAndroidGenymotionAndroidVersion(String value) {
        this.androidGenymotionAndroidVersion.setValue(value);
    }

    public String getAndroidGenymotionAndroidApi() {
        return resolve(androidGenymotionAndroidApi);
    }

    public void setAndroidGenymotionAndroidApi(String value) {
        this.androidGenymotionAndroidApi.setValue(value);
    }

    public String getAndroidGenymotionScreenSize() {
        return resolve(androidGenymotionScreenSize);
    }

    public void setAndroidGenymotionScreenSize(String value) {
        this.androidGenymotionScreenSize.setValue(value);
    }

    public String getAndroidGenymotionChromedriver() {
        return resolve(androidGenymotionChromedriver);
    }

    public void setAndroidGenymotionChromedriver(String value) {
        this.androidGenymotionChromedriver.setValue(value);
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

    public Integer getAndroidDeviceTimeoutSec() {
        return resolve(androidDeviceTimeoutSec);
    }

    public void setAndroidDeviceTimeoutSec(Integer value) {
        this.androidDeviceTimeoutSec.setValue(value);
    }

    public Integer getAndroidDeviceStartupTimeoutSec() {
        return resolve(androidDeviceStartupTimeoutSec);
    }

    public void setAndroidDeviceStartupTimeoutSec(Integer value) {
        this.androidDeviceStartupTimeoutSec.setValue(value);
    }

    public Integer getAndroidAppiumPingPeriodSec() {
        return resolve(androidAppiumPingPeriodSec);
    }

    public void setAndroidAppiumPingPeriodSec(
            Integer androidAppiumPingPeriodSec) {
        this.androidAppiumPingPeriodSec.setValue(androidAppiumPingPeriodSec);
    }

    public boolean isAndroidLogging() {
        return resolve(androidLogging);
    }

    public void setAndroidLogging(boolean value) {
        this.androidLogging.setValue(value);
    }

    public String getAndroidLogsFolder() {
        return resolve(androidLogsFolder);
    }

    public void setAndroidLogsFolder(String androidLogsFolder) {
        this.androidLogsFolder.setValue(androidLogsFolder);
    }

    public String getAndroidAppiumLogLevel() {
        return resolve(androidAppiumLogLevel);
    }

    public void setAndroidAppiumLogLevel(String androidAppiumLogLevel) {
        this.androidAppiumLogLevel.setValue(androidAppiumLogLevel);
    }

    public String getAndroidAppiumLogFile() {
        return resolve(androidAppiumLogFile);
    }

    public void setAndroidAppiumLogFile(String androidAppiumLogFile) {
        this.androidAppiumLogFile.setValue(androidAppiumLogFile);
    }

    public String getAndroidScreenWidth() {
        return resolve(androidScreenWidth);
    }

    public void setAndroidScreenWidth(String value) {
        this.androidScreenWidth.setValue(value);
    }

    public String getAndroidScreenHeigth() {
        return resolve(androidScreenHeigth);
    }

    public void setAndroidScreenHeigth(String value) {
        this.androidScreenHeigth.setValue(value);
    }

    public String getAndroidScreenDepth() {
        return resolve(androidScreenDepth);
    }

    public void setAndroidScreenDepth(String value) {
        this.androidScreenDepth.setValue(value);
    }

    public int getServerPort() {
        return resolve(serverPort);
    }

    public Config setServerPort(int value) {
        this.serverPort.setValue(value);
        return this;
    }

    public String getServerPath() {
        return resolve(serverPath);
    }

    public Config setServerPath(String value) {
        this.serverPath.setValue(value);
        return this;
    }

    public int getServerTimeoutSec() {
        return resolve(serverTimeoutSec);
    }

    public Config setServerTimeoutSec(int value) {
        this.serverTimeoutSec.setValue(value);
        return this;
    }

    public int getRemoteWebdriverWaitTimeoutSec() {
        return resolve(remoteWebdriverWaitTimeoutSec);
    }

    public Config setRemoteWebdriverWaitTimeoutSec(int value) {
        this.remoteWebdriverWaitTimeoutSec.setValue(value);
        return this;
    }

    public int getRemoteWebdriverPollTimeSec() {
        return resolve(remoteWebdriverPollTimeSec);
    }

    public Config setRemoteWebdriverPollTimeSec(int value) {
        this.remoteWebdriverPollTimeSec.setValue(value);
        return this;
    }

    public int getTtlSec() {
        return resolve(ttlSec);
    }

    public Config setTtlSec(int value) {
        this.ttlSec.setValue(value);
        return this;
    }

    public boolean isUseDockerCache() {
        return resolve(useDockerCache);
    }

    public Config setUseDockerCache(boolean value) {
        this.useDockerCache.setValue(value);
        return this;
    }

    public String getCachePath() {
        return resolvePath(resolve(cachePath));
    }

    private String resolvePath(String path) {
        if (path != null) {
            // Partial support for Bash tilde expansion:
            // http://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
            if (path.startsWith(HOME + '/')) {
                path = Paths
                        .get(System.getProperty("user.home"), path.substring(1))
                        .toString();
            } else if (path.equals(".")) {
                path = Paths.get("").toAbsolutePath().toString();
            }
        }
        return path;
    }

    public Config setCachePath(String value) {
        this.cachePath.setValue(value);
        return this;
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

    public WebDriverManager chromedriver() {
        return WebDriverManager.chromedriver();
    }

    public WebDriverManager edgedriver() {
        return WebDriverManager.edgedriver();
    }

    public WebDriverManager firefoxdriver() {
        return WebDriverManager.firefoxdriver();
    }

    public WebDriverManager iedriver() {
        return WebDriverManager.iedriver();
    }

    public WebDriverManager operadriver() {
        return WebDriverManager.operadriver();
    }

    public WebDriverManager phantomjs() {
        return WebDriverManager.phantomjs();
    }
}
