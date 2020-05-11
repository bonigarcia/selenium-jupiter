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
package io.github.bonigarcia.seljup.handler;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.spotify.docker.client.messages.PortBinding.randomPort;
import static io.github.bonigarcia.seljup.BrowserType.ANDROID;
import static io.github.bonigarcia.seljup.BrowserType.EDGE;
import static io.github.bonigarcia.seljup.BrowserType.IEXPLORER;
import static io.github.bonigarcia.seljup.BrowserType.OPERA;
import static io.github.bonigarcia.seljup.CloudType.GENYMOTION_SAAS;
import static io.github.bonigarcia.seljup.CloudType.NONE;
import static io.github.bonigarcia.seljup.SurefireReports.getOutputFolder;
import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getenv;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.move;
import static java.nio.file.Files.write;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.collections.CollectionUtils.disjunction;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.openqa.selenium.chrome.ChromeOptions.CAPABILITY;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;

import com.codeborne.selenide.SelenideDriver;
import com.google.gson.GsonBuilder;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.PortBinding;

import io.appium.java_client.android.AndroidDriver;
import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.BrowserInstance;
import io.github.bonigarcia.seljup.BrowserType;
import io.github.bonigarcia.seljup.CloudType;
import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.DockerContainer;
import io.github.bonigarcia.seljup.DockerContainer.DockerBuilder;
import io.github.bonigarcia.seljup.DockerService;
import io.github.bonigarcia.seljup.InternalPreferences;
import io.github.bonigarcia.seljup.SeleniumJupiterException;
import io.github.bonigarcia.seljup.SelenoidConfig;
import io.github.bonigarcia.seljup.WebDriverCreator;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Resolver for DockerDriver's.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class DockerDriverHandler {

    static final String ALL_IPV4_ADDRESSES = "0.0.0.0";
    static final String LATEST = "latest";
    static final String BETA = "beta";
    static final String BROWSER = "browser";
    static final String CHROME = "chrome";
    static final String OPERA_NAME = "operablink";
    static final int APPIUM_MIN_PING_SEC = 5;

    final Logger log = getLogger(lookup().lookupClass());

    Config config;
    DockerService dockerService;
    SelenoidConfig selenoidConfig;
    Map<String, DockerContainer> containerMap;
    Map<String, String[]> finalizerCommandMap;
    File recordingFile;
    String name;
    File hostVideoFolder;
    ExtensionContext context;
    Parameter parameter;
    Optional<Object> testInstance;
    AnnotationsReader annotationsReader;
    String index;
    String androidNoVncUrl;
    List<File> filesInVideoFolder;
    String browserName;
    WebDriverCreator webDriverCreator;
    URL hubUrl;
    File hostAndroidLogsFolder;
    URL remoteUrl;
    String browserVersion;

    public DockerDriverHandler(Config config, BrowserInstance browserInstance,
            String version, InternalPreferences preferences) {
        this.config = config;
        this.selenoidConfig = new SelenoidConfig(config, browserInstance,
                version);
        this.dockerService = new DockerService(config, preferences);
        this.containerMap = new LinkedHashMap<>();
        this.finalizerCommandMap = new LinkedHashMap<>();
        this.testInstance = empty();
        this.browserVersion = version;
    }

    public DockerDriverHandler(ExtensionContext context, Parameter parameter,
            Optional<Object> testInstance, AnnotationsReader annotationsReader,
            Map<String, DockerContainer> containerMap,
            DockerService dockerService, Config config,
            BrowserInstance browserInstance, String version) {
        this.context = context;
        this.parameter = parameter;
        this.testInstance = testInstance;
        this.annotationsReader = annotationsReader;
        this.containerMap = containerMap;
        this.finalizerCommandMap = new LinkedHashMap<>();
        this.dockerService = dockerService;
        this.config = config;
        this.selenoidConfig = new SelenoidConfig(getConfig(), browserInstance,
                version);
        this.browserVersion = version;
    }

    public WebDriver resolve(DockerBrowser dockerBrowser) {
        BrowserType browserType = dockerBrowser.type();
        CloudType cloudType = dockerBrowser.cloud();
        BrowserInstance browserInstance = new BrowserInstance(config,
                annotationsReader, browserType, cloudType,
                Optional.ofNullable(dockerBrowser.browserName()),
                Optional.ofNullable(dockerBrowser.volumes()));
        String version = dockerBrowser.version();
        String deviceName = dockerBrowser.deviceName();
        String url = dockerBrowser.url();

        return resolve(browserInstance, version, deviceName, url, true);
    }

    public WebDriver resolve(BrowserInstance browserInstance, String version,
            String deviceName, String url, boolean createWebDriver) {
        BrowserType browserType = browserInstance.getBrowserType();
        try {
            if (url != null && !url.isEmpty()) {
                remoteUrl = new URL(url);
                dockerService.updateDockerClient(url);
            }
            if (getConfig().isRecording()
                    || getConfig().isRecordingWhenFailure()) {
                hostVideoFolder = new File(getOutputFolder(context,
                        getConfig().getOutputFolder()));
            }

            WebDriver webdriver;
            if (browserType == ANDROID) {
                webdriver = getDriverForAndroid(browserInstance, version,
                        deviceName);
            } else {
                webdriver = getDriverForBrowser(browserInstance, version,
                        createWebDriver);
            }
            return webdriver;

        } catch (Exception e) {
            String errorMessage = format(
                    "Exception resolving driver in Docker (%s %s)", browserType,
                    version);
            throw new SeleniumJupiterException(errorMessage, e);
        }
    }

    private boolean isAndroidLogging() {
        boolean androidLogging = getConfig().isAndroidLogging();
        if (androidLogging) {
            String dateTime = DateTimeFormatter
                    .ofPattern("uuuu-MM-dd--HH-mm-ss")
                    .format(LocalDateTime.now());
            String logsFolder = getConfig().getAndroidLogsFolder();
            Path path = Paths.get(
                    getOutputFolder(context, getConfig().getOutputFolder()),
                    logsFolder, dateTime);
            try {
                Files.createDirectories(path);
                hostAndroidLogsFolder = path.toFile();
                log.debug("Android logs will be stored in {}",
                        hostAndroidLogsFolder);
            } catch (IOException e) {
                log.warn("Failed to create directories for Android logs {}",
                        path.toAbsolutePath(), e);
                androidLogging = false;
            }
        }
        return androidLogging;
    }

    private WebDriver getDriverForBrowser(BrowserInstance browserInstance,
            String version, boolean createWebDriver)
            throws IllegalAccessException, IOException, DockerException,
            InterruptedException {
        boolean enableVnc = getConfig().isVnc();
        DesiredCapabilities capabilities = getCapabilities(browserInstance,
                enableVnc);
        BrowserType browserType = browserInstance.getBrowserType();

        String imageVersion;
        String versionFromLabel = version;
        if (version != null && !version.isEmpty()
                && !version.equalsIgnoreCase(LATEST)) {
            if (version.startsWith(LATEST + "-")) {
                versionFromLabel = selenoidConfig.getDockerBrowserConfig()
                        .getVersion();
            }
            imageVersion = selenoidConfig.getImageVersion(browserType,
                    versionFromLabel);
            if (!imageVersion.equalsIgnoreCase(BETA)) {
                capabilities.setCapability("version", imageVersion);
            }
        } else {
            imageVersion = selenoidConfig.getDefaultBrowser(browserType);
        }

        boolean seleniumServerUrlAvailable = setHubUrl(browserInstance,
                versionFromLabel);

        if (!createWebDriver) {
            return null;
        }

        if (webDriverCreator == null) {
            webDriverCreator = new WebDriverCreator(getConfig());
        }
        log.trace("Creating webdriver for {} {} ({})", browserType, version,
                hubUrl);
        WebDriver webdriver = webDriverCreator.createRemoteWebDriver(hubUrl,
                capabilities);

        SessionId sessionId = ((RemoteWebDriver) webdriver).getSessionId();
        updateName(browserType, imageVersion, webdriver);

        if (enableVnc && !seleniumServerUrlAvailable) {
            String selenoidHost = hubUrl.getHost();
            int selenoidPort = hubUrl.getPort();

            String novncUrl = getNoVncUrl(selenoidHost, selenoidPort,
                    sessionId.toString(), getConfig().getSelenoidVncPassword());
            logSessionId(sessionId);
            logNoVncUrl(novncUrl);

            String vncExport = getConfig().getVncExport();
            log.trace("Exporting VNC URL as Java property {}", vncExport);
            System.setProperty(vncExport, novncUrl);

            if (getConfig().isVncRedirectHtmlPage()) {
                String outputFolder = getOutputFolder(context,
                        getConfig().getOutputFolder());
                String vncHtmlPage = format("<!DOCTYPE html>\n" + "<html>\n"
                        + "<head>\n"
                        + "<meta http-equiv=\"refresh\" content=\"0; url=%s\">\n"
                        + "</head>\n" + "<body>\n" + "</body>\n" + "</html>",
                        novncUrl);
                String htmlPageName = name + ".html";
                log.debug("Redirecting VNC URL to HTML page at {}/{}",
                        outputFolder, htmlPageName);
                write(Paths.get(outputFolder, htmlPageName),
                        vncHtmlPage.getBytes());
            }
        }

        if (getConfig().isRecording() || getConfig().isRecordingWhenFailure()) {
            recordingFile = new File(hostVideoFolder, sessionId + ".mp4");
        }
        return webdriver;
    }

    private boolean setHubUrl(BrowserInstance browserInstance,
            String versionFromLabel) throws MalformedURLException,
            DockerException, InterruptedException {
        String seleniumServerUrl = getConfig().getSeleniumServerUrl();
        boolean seleniumServerUrlAvailable = seleniumServerUrl != null
                && !seleniumServerUrl.isEmpty();
        hubUrl = new URL(seleniumServerUrlAvailable ? seleniumServerUrl
                : startDockerBrowser(browserInstance, versionFromLabel));
        if (remoteUrl != null) {
            try {
                String remoteHost = remoteUrl.getHost();
                log.trace("Converting {} to use {}", hubUrl, remoteHost);
                URI uri = new URI(hubUrl.toString());
                hubUrl = new URI(uri.getScheme(), null, remoteHost,
                        uri.getPort(), uri.getPath(), uri.getQuery(),
                        uri.getFragment()).toURL();
            } catch (URISyntaxException e) {
                log.warn("Exception converting URL {}", remoteUrl, e);
            }
        }
        return seleniumServerUrlAvailable;
    }

    private void logSessionId(SessionId sessionId) {
        log.info("Session id {}", sessionId);
    }

    private void logNoVncUrl(String novncUrl) {
        log.info(
                "VNC URL (copy and paste in a browser navigation bar to interact with remote session)");
        log.info("{}", novncUrl);
    }

    private WebDriver getDriverForAndroid(BrowserInstance browserInstance,
            String version, String deviceName) throws DockerException,
            InterruptedException, IOException, IllegalAccessException {
        if (getConfig().isRecording() || getConfig().isRecordingWhenFailure()) {
            filesInVideoFolder = asList(hostVideoFolder.listFiles());
        }
        if (version == null || version.isEmpty()) {
            version = getConfig().getAndroidDefaultVersion();
        }
        String deviceNameCapability = deviceName != null
                && !deviceName.isEmpty() ? deviceName
                        : getConfig().getAndroidDeviceName();
        CloudType cloudType = browserInstance.getCloudType();
        String appiumUrl = startAndroidBrowser(version, deviceNameCapability,
                browserInstance.getBrowserName(), cloudType);

        DesiredCapabilities capabilities = getCapabilitiesForAndroid(
                browserInstance, deviceNameCapability);
        capabilities.setBrowserName(browserName);

        log.info("Appium URL in Android device: {}", appiumUrl);
        log.info("Android device name: {} -- Browser: {}", deviceNameCapability,
                browserName);

        int androidStartupTimeoutSec = getConfig()
                .getAndroidDeviceStartupTimeoutSec();
        if (0 < androidStartupTimeoutSec) {
            log.debug("Waiting for Android device to start for {} seconds",
                    androidStartupTimeoutSec);
            sleep(SECONDS.toMillis(androidStartupTimeoutSec));
        }

        int androidAppiumPingPeriodSec = getConfig()
                .getAndroidAppiumPingPeriodSec();
        if (androidAppiumPingPeriodSec < APPIUM_MIN_PING_SEC) {
            androidAppiumPingPeriodSec = APPIUM_MIN_PING_SEC;
        }
        log.debug(
                "Waiting for Appium creates session in Android device ... this might take long, please wait (retries each {} seconds)",
                androidAppiumPingPeriodSec);

        AndroidDriver<WebElement> androidDriver = null;
        int androidDeviceTimeoutSec = getConfig().getAndroidDeviceTimeoutSec();
        long endTimeMillis = currentTimeMillis()
                + androidDeviceTimeoutSec * 1000;
        do {
            try {
                androidDriver = new AndroidDriver<>(new URL(appiumUrl),
                        capabilities);
            } catch (Exception e) {
                checkAndroidException(androidAppiumPingPeriodSec,
                        androidDeviceTimeoutSec, endTimeMillis, e);
            }
        } while (androidDriver == null);
        log.info("Android device ready {}", androidDriver);
        updateName(browserInstance.getBrowserType(), version, androidDriver);

        if (getConfig().isVnc()) {
            logSessionId(androidDriver.getSessionId());
            logNoVncUrl(androidNoVncUrl);
        }
        return androidDriver;
    }

    private void checkAndroidException(int androidAppiumPingPeriodSec,
            int androidDeviceTimeoutSec, long endTimeMillis, Exception e)
            throws InterruptedException {
        if (currentTimeMillis() > endTimeMillis) {
            throw new SeleniumJupiterException(
                    "Timeout (" + androidDeviceTimeoutSec
                            + " seconds) waiting for Android device in Docker");
        }
        String errorMessage = getErrorMessage(e);
        log.debug("Android device not ready: {}", errorMessage);
        if (errorMessage.contains("Could not find package")) {
            throw new SeleniumJupiterException(errorMessage);
        }
        sleep(SECONDS.toMillis(androidAppiumPingPeriodSec));
    }

    private String getErrorMessage(Exception e) {
        String errorMessage = getRootCause(e).getMessage();
        int i = errorMessage.indexOf('\n');
        if (i != -1) {
            errorMessage = errorMessage.substring(0, i);
        }
        return errorMessage;
    }

    private void updateName(BrowserType browser, String imageVersion,
            WebDriver webdriver) {
        if (parameter != null) {
            String parameterName = parameter.getName();
            name = parameterName + "_" + browser + "_" + imageVersion + "_"
                    + ((RemoteWebDriver) webdriver).getSessionId();
            Optional<Method> testMethod = context.getTestMethod();
            if (testMethod.isPresent()) {
                name = testMethod.get().getName() + "_" + name;
            }
            if (index != null) {
                name += index;
            }
        } else {
            name = browser.name().toLowerCase();
        }
    }

    private DesiredCapabilities getCapabilities(BrowserInstance browserInstance,
            boolean enableVnc) throws IllegalAccessException, IOException {
        DesiredCapabilities capabilities = browserInstance.getCapabilities();
        if (enableVnc) {
            capabilities.setCapability("enableVNC", true);
            capabilities.setCapability("screenResolution",
                    getConfig().getVncScreenResolution());
        }

        if (getConfig().isRecording() || getConfig().isRecordingWhenFailure()) {
            capabilities.setCapability("enableVideo", true);
            capabilities.setCapability("videoScreenSize",
                    getConfig().getRecordingVideoScreenSize());
            capabilities.setCapability("videoFrameRate",
                    getConfig().getRecordingVideoFrameRate());
        }

        Optional<Capabilities> optionalCapabilities = annotationsReader != null
                ? annotationsReader.getCapabilities(parameter, testInstance)
                : Optional.of(new DesiredCapabilities());
        MutableCapabilities options = browserInstance.getDriverHandler()
                .getOptions(parameter, testInstance);

        // Due to bug in operablink the binary path must be set
        if (browserInstance.getBrowserType() == OPERA
                && browserVersion.equals("62.0")) {
            String operaBinaryPathLinux = getConfig().getOperaBinaryPathLinux();
            ((OperaOptions) options).setBinary(operaBinaryPathLinux);
            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.setBinary(operaBinaryPathLinux);

            OperaOptions operaOptions = new OperaOptions().merge(chromeOptions);
            operaOptions.setCapability("browserName", OPERA_NAME);
            options.merge(operaOptions);
            log.trace("Opera options: {}", options);
        }

        if (optionalCapabilities.isPresent()) {
            options.merge(optionalCapabilities.get());
        }
        capabilities.setCapability(browserInstance.getOptionsKey(), options);
        log.trace("Using {}", capabilities);
        return capabilities;
    }

    private DesiredCapabilities getCapabilitiesForAndroid(
            BrowserInstance browserInstance, String deviceNameCapability)
            throws IllegalAccessException, IOException {
        DesiredCapabilities capabilities = browserInstance.getCapabilities();
        capabilities.setCapability("browserName", browserName);
        capabilities.setCapability("deviceName", deviceNameCapability);

        Optional<Capabilities> optionalCapabilities = annotationsReader != null
                ? annotationsReader.getCapabilities(parameter, testInstance)
                : Optional.of(new DesiredCapabilities());
        MutableCapabilities options = browserInstance.getDriverHandler()
                .getOptions(parameter, testInstance);

        if (optionalCapabilities.isPresent()) {
            options.merge(optionalCapabilities.get());
        }
        capabilities.setCapability(CAPABILITY, options);
        log.trace("Using {}", capabilities);
        return capabilities;
    }

    public String getName() {
        return name;
    }

    public void cleanup() {
        try {
            boolean recordingWhenFailure = getConfig().isRecordingWhenFailure();
            boolean recording = getConfig().isRecording();

            // Wait for recordings
            if (recording || recordingWhenFailure) {
                waitForRecording();
                boolean isFailed = context.getExecutionException().isPresent();
                if (recordingWhenFailure && !isFailed) {
                    log.trace("Deleting {} (recordingWhenFailure={})",
                            recordingFile, recordingWhenFailure);
                    delete(recordingFile.toPath());
                }
            }
            // Clear VNC URL
            String vncExport = getConfig().getVncExport();
            if (getConfig().isVnc() && System.getProperty(vncExport) != null) {
                log.trace("Clearing Java property {}", vncExport);
                System.clearProperty(vncExport);
            }
        } catch (Exception e) {
            log.warn("Exception waiting for recording {}", e.getMessage());
        } finally {
            // Execute finalize command in docker container (if any)
            finaliceContainers();

            // Stop containers
            stopContainers();
        }
    }

    private void stopContainers() {
        if (containerMap != null && !containerMap.isEmpty()
                && dockerService != null) {
            int numContainers = containerMap.size();
            log.trace("There are {} container(s): {}", numContainers,
                    containerMap);

            if (numContainers > 0) {
                ExecutorService executorService = newFixedThreadPool(
                        numContainers);
                CountDownLatch latch = new CountDownLatch(numContainers);
                for (Map.Entry<String, DockerContainer> entry : containerMap
                        .entrySet()) {
                    executorService.submit(() -> {
                        dockerService.stopAndRemoveContainer(
                                entry.getValue().getContainerId(),
                                entry.getKey());
                        latch.countDown();
                    });
                }
                containerMap.clear();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    currentThread().interrupt();
                }
                executorService.shutdown();
            }
        }
    }

    private void finaliceContainers() {
        if (finalizerCommandMap != null && !finalizerCommandMap.isEmpty()
                && dockerService != null) {
            for (Map.Entry<String, String[]> entry : finalizerCommandMap
                    .entrySet()) {
                String container = entry.getKey();
                String[] command = entry.getValue();
                try {
                    log.trace("Executing {} in {}", command, container);
                    dockerService.execCommandInContainer(container, command);
                } catch (Exception e) {
                    log.warn("Exception executing {} in {}", command, container,
                            e);
                }
            }
        }
    }

    public void close() {
        dockerService.close();
    }

    public String startAndroidBrowser(String version, String deviceName,
            String browserNameSetByUser, CloudType cloudType)
            throws DockerException, InterruptedException {
        if (!IS_OS_LINUX) {
            throw new SeleniumJupiterException(
                    "Android devices are only supported in Linux hosts");
        }

        String androidImage;

        if (cloudType == NONE) {
            String versionTag;
            String apiLevel;

            switch (version) {
            case "5.0.1":
            case LATEST + "-7":
                androidImage = getConfig().getAndroidImage501();
                apiLevel = "21";
                browserName = BROWSER;
                versionTag = "37.0";
                break;
            case "5.1.1":
            case LATEST + "-6":
                androidImage = getConfig().getAndroidImage511();
                apiLevel = "22";
                browserName = BROWSER;
                versionTag = "39.0";
                break;
            case "6.0":
            case LATEST + "-5":
                androidImage = getConfig().getAndroidImage60();
                apiLevel = "23";
                browserName = BROWSER;
                versionTag = "44.0";
                break;
            case "7.0":
            case LATEST + "-4":
                androidImage = getConfig().getAndroidImage701();
                apiLevel = "24";
                browserName = CHROME;
                versionTag = "51.0";
                break;
            case "7.1.1":
            case LATEST + "-3":
                androidImage = getConfig().getAndroidImage711();
                apiLevel = "25";
                browserName = CHROME;
                versionTag = "55.0";
                break;
            case "8.0":
            case LATEST + "-2":
                androidImage = getConfig().getAndroidImage80();
                apiLevel = "26";
                browserName = CHROME;
                versionTag = "58.0";
                break;
            case "8.1":
            case LATEST + "-1":
                androidImage = getConfig().getAndroidImage81();
                apiLevel = "27";
                browserName = CHROME;
                versionTag = "61.0";
                break;
            case "9.0":
            case LATEST:
                androidImage = getConfig().getAndroidImage90();
                apiLevel = "28";
                browserName = CHROME;
                versionTag = "66.0";
                break;
            default:
                throw new SeleniumJupiterException("Version " + version
                        + " not valid for Android devices");
            }
            log.info("Starting {} {} in Android {} (API level {})", browserName,
                    versionTag, version, apiLevel);

        } else {
            androidImage = getConfig().getAndroidImageGenymotion();
            browserName = browserNameSetByUser;
        }

        dockerService.pullImage(androidImage);

        DockerContainer androidContainer = startAndroidContainer(androidImage,
                deviceName, cloudType);
        return androidContainer.getContainerUrl();

    }

    public String startDockerBrowser(BrowserInstance browserInstance,
            String version) throws DockerException, InterruptedException {

        String browserImage;
        BrowserType browserType = browserInstance.getBrowserType();
        if (version == null || version.isEmpty()
                || version.equalsIgnoreCase(LATEST)) {
            log.info("Using {} version {} (latest)", browserType,
                    selenoidConfig.getDefaultBrowser(browserType));
            browserImage = selenoidConfig.getLatestImage(browserInstance);
        } else {
            log.info("Using {} version {}", browserType, version);
            browserImage = selenoidConfig.getImageFromVersion(browserType,
                    version);
        }
        if (browserType != EDGE && browserType != IEXPLORER) {
            dockerService.pullImage(browserImage);
        }

        DockerContainer selenoidContainer = startSelenoidContainer();
        return selenoidContainer.getContainerUrl();
    }

    public DockerContainer startSelenoidContainer()
            throws DockerException, InterruptedException {

        DockerContainer selenoidContainer;
        String selenoidImage = getConfig().getSelenoidImage();
        boolean recording = getConfig().isRecording()
                || getConfig().isRecordingWhenFailure();

        if (containerMap.containsKey(selenoidImage)) {
            log.trace("Selenoid container already available");
            selenoidContainer = containerMap.get(selenoidImage);
        } else {
            // Pull images
            dockerService.pullImage(selenoidImage);
            String recordingImage = getConfig().getRecordingImage();
            if (recording) {
                dockerService.pullImage(recordingImage);
            }

            // portBindings
            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            String defaultSelenoidPort = getConfig().getSelenoidPort();
            String internalSelenoidPort = defaultSelenoidPort;
            portBindings.put(internalSelenoidPort,
                    asList(randomPort(ALL_IPV4_ADDRESSES)));

            // binds
            String defaultSocket = dockerService.getDockerDefaultSocket();
            List<String> binds = new ArrayList<>();
            binds.add(defaultSocket + ":" + defaultSocket);
            if (recording) {
                binds.add(getDockerPath(hostVideoFolder)
                        + ":/opt/selenoid/video");
            }

            // entrypoint & cmd
            List<String> entryPoint = asList("");
            String internalBrowserPort = getConfig().getSelenoidPort();
            String browsersJson = selenoidConfig.getBrowsersJsonAsString();
            String browserTimeout = getConfig()
                    .getBrowserSessionTimeoutDuration();
            String network = getConfig().getDockerNetwork();
            String dockerStartupTimeout = getConfig()
                    .getDockerStartupTimeoutDuration();

            List<String> cmd = asList("sh", "-c",
                    "mkdir -p /etc/selenoid/; echo '" + browsersJson
                            + "' > /etc/selenoid/browsers.json; /usr/bin/selenoid"
                            + " -listen :" + internalBrowserPort
                            + " -service-startup-timeout "
                            + dockerStartupTimeout
                            + " -conf /etc/selenoid/browsers.json"
                            + " -video-output-dir /opt/selenoid/video/"
                            + " -timeout " + browserTimeout
                            + " -container-network " + network + " -limit "
                            + getDockerBrowserCount());

            // envs
            List<String> envs = selenoidConfig.getDockerEnvs();

            if (recording) {
                envs.add("OVERRIDE_VIDEO_OUTPUT_DIR="
                        + getDockerPath(hostVideoFolder));
            }

            // Build container
            DockerBuilder dockerBuilder = DockerContainer
                    .dockerBuilder(selenoidImage).portBindings(portBindings)
                    .binds(binds).cmd(cmd).entryPoint(entryPoint).envs(envs)
                    .network(network);
            selenoidContainer = dockerBuilder.build();
            containerMap.put(selenoidImage, selenoidContainer);

            String containerId = dockerService
                    .startContainer(selenoidContainer);
            selenoidContainer.setContainerId(containerId);
            String selenoidHost = dockerService.getHost(containerId, network);
            String selenoidPort = dockerService.getBindPort(containerId,
                    internalSelenoidPort + "/tcp");
            String selenoidUrl = format("http://%s:%s/wd/hub", selenoidHost,
                    selenoidPort);

            selenoidContainer.setContainerUrl(selenoidUrl);
            log.trace("Selenium server URL {}", selenoidUrl);
        }
        return selenoidContainer;
    }

    public DockerContainer startAndroidContainer(String androidImage,
            String deviceName, CloudType cloudType)
            throws DockerException, InterruptedException {

        DockerContainer androidContainer;
        if (containerMap.containsKey(androidImage)) {
            log.trace("Android container already available");
            androidContainer = containerMap.get(androidImage);
        } else {
            // Pull image
            dockerService.pullImage(androidImage);

            // portBindings
            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            String internalAppiumPort = getConfig().getAndroidAppiumPort();
            portBindings.put(internalAppiumPort,
                    asList(randomPort(ALL_IPV4_ADDRESSES)));
            String internalNoVncPort = getConfig().getAndroidNoVncPort();
            portBindings.put(internalNoVncPort,
                    asList(randomPort(ALL_IPV4_ADDRESSES)));

            // binds
            boolean recording = getConfig().isRecording()
                    || getConfig().isRecordingWhenFailure();
            List<String> binds = new ArrayList<>();
            if (recording) {
                binds.add(getDockerPath(hostVideoFolder) + ":/tmp/video");
            }
            if (isAndroidLogging()) {
                binds.add(getDockerPath(hostAndroidLogsFolder)
                        + ":/var/log/supervisor");
            }

            // envs
            String network = getConfig().getDockerNetwork();
            List<String> envs = getAndroidEnvs(deviceName, cloudType,
                    recording);

            // Build container
            DockerBuilder dockerBuilder = DockerContainer
                    .dockerBuilder(androidImage).portBindings(portBindings)
                    .binds(binds).envs(envs).network(network).privileged();

            String androidGenymotionDeviceName = getConfig()
                    .getAndroidGenymotionDeviceName();
            boolean useGenymotion = cloudType == GENYMOTION_SAAS
                    && !isNullOrEmpty(androidGenymotionDeviceName);
            if (useGenymotion) {
                getGenymotionContainer(dockerBuilder,
                        androidGenymotionDeviceName);
            }

            androidContainer = dockerBuilder.build();
            String containerId = dockerService.startContainer(androidContainer);

            if (useGenymotion) {
                String[] disposeDeviceCommand = { "gmtool", "--cloud", "admin",
                        "stopdisposable", androidGenymotionDeviceName };
                finalizerCommandMap.put(containerId, disposeDeviceCommand);
            }

            String androidHost = dockerService.getHost(containerId, network);
            String androidPort = dockerService.getBindPort(containerId,
                    internalAppiumPort + "/tcp");

            String appiumUrl = format("http://%s:%s/wd/hub", androidHost,
                    androidPort);
            androidContainer.setContainerId(containerId);
            androidContainer.setContainerUrl(appiumUrl);

            String androidNoVncPort = dockerService.getBindPort(containerId,
                    internalNoVncPort + "/tcp");
            androidNoVncUrl = format("http://%s:%s/", androidHost,
                    androidNoVncPort);

            containerMap.put(androidImage, androidContainer);
        }
        return androidContainer;
    }

    private void getGenymotionContainer(DockerBuilder dockerBuilder,
            String androidGenymotionDeviceName) {
        Devices[] devices = new Devices[1];
        String androidGenymotionTemplate = getConfig()
                .getAndroidGenymotionTemplate();
        String androidGenymotionAndroidVersion = getConfig()
                .getAndroidGenymotionAndroidVersion();
        if (!isNullOrEmpty(androidGenymotionAndroidVersion)) {
            androidGenymotionTemplate += " - "
                    + androidGenymotionAndroidVersion;
        }
        String androidGenymotionAndroidApi = getConfig()
                .getAndroidGenymotionAndroidApi();
        if (!isNullOrEmpty(androidGenymotionAndroidApi)) {
            androidGenymotionTemplate += " - API "
                    + androidGenymotionAndroidApi;
        }
        String androidGenymotionScreenSize = getConfig()
                .getAndroidGenymotionScreenSize();
        if (!isNullOrEmpty(androidGenymotionScreenSize)) {
            androidGenymotionTemplate += " - " + androidGenymotionScreenSize;
        }

        log.debug("Using Genymotion device name: {}, template: {}",
                androidGenymotionDeviceName, androidGenymotionTemplate);
        devices[0] = new Devices(androidGenymotionDeviceName,
                androidGenymotionTemplate);
        String deviceJson = new GsonBuilder().disableHtmlEscaping().create()
                .toJson(devices);

        String chromedriverVersion = getConfig()
                .getAndroidGenymotionChromedriver();
        if (isNullOrEmpty(chromedriverVersion)
                && !isNullOrEmpty(androidGenymotionAndroidVersion)) {
            switch (androidGenymotionAndroidVersion) {
            case "5.0.1":
                chromedriverVersion = "2.21";
                break;
            case "5.1.1":
                chromedriverVersion = "2.13";
                break;
            case "6.0":
            case "6.0.0":
                chromedriverVersion = "2.18";
                break;
            case "7.0":
            case "7.0.0":
                chromedriverVersion = "2.23";
                break;
            case "7.1.1":
                chromedriverVersion = "2.28";
                break;
            case "8.0":
            case "8.0.0":
                chromedriverVersion = "2.31";
                break;
            case "8.1":
            case "8.1.0":
                chromedriverVersion = "2.33";
                break;
            case "9.0":
            case "9.0.0":
                chromedriverVersion = "2.40";
                break;
            default:
                chromedriverVersion = "";
                break;
            }
        }
        String downloadChromeDriverScript = "";
        if (!isNullOrEmpty(chromedriverVersion)) {
            log.debug(
                    "Chromedriver {} is downloaded inside Genymotion container",
                    chromedriverVersion);
            downloadChromeDriverScript = "wget https://chromedriver.storage.googleapis.com/"
                    + chromedriverVersion + "/chromedriver_linux64.zip; "
                    + "unzip chromedriver_linux64.zip; "
                    + "cp chromedriver /usr/lib/node_modules/appium/node_modules/appium-chromedriver/chromedriver/linux/chromedriver_64; "
                    + "rm chromedriver*; ";
        }

        log.trace("Devices.json = {}", deviceJson);
        List<String> cmd = asList("sh", "-c",
                "mkdir /root/tmp; echo '" + deviceJson
                        + "' > /root/tmp/devices.json; "
                        + downloadChromeDriverScript + "./geny_start.sh");
        dockerBuilder.cmd(cmd);
    }

    private List<String> getAndroidEnvs(String deviceName, CloudType cloudType,
            boolean recording) {
        List<String> envs = new ArrayList<>();
        String screenWidth = getConfig().getAndroidScreenWidth();
        String screenHeigth = getConfig().getAndroidScreenHeigth();
        String screenDepth = getConfig().getAndroidScreenDepth();
        envs.add("DEVICE=" + deviceName);
        envs.add("SCREEN_WIDTH=" + screenWidth);
        envs.add("SCREEN_HEIGHT=" + screenHeigth);
        envs.add("SCREEN_DEPTH=" + screenDepth);
        envs.add("APPIUM=true");
        List<String> proxyEnvVars = asList("HTTP_PROXY", "HTTPS_PROXY",
                "NO_PROXY", "http_proxy", "https_proxy", "no_proxy");
        proxyEnvVars.stream().filter(envName -> isNotBlank(getenv(envName)))
                .forEach(envName -> envs.add(envName + "=" + getenv(envName)));
        if (recording) {
            envs.add("AUTO_RECORD=true");
        } else {
            envs.add("AUTO_RECORD=false");
        }

        if (cloudType == GENYMOTION_SAAS) {
            envs.add("TYPE=SaaS");
            envs.add("USER=" + getConfig().getAndroidGenymotionUser());
            envs.add("PASS=" + getConfig().getAndroidGenymotionPassword());
            envs.add("LICENSE=" + getConfig().getAndroidGenymotionLicense());
        }
        return envs;
    }

    private int getDockerBrowserCount() {
        int count = 0;
        if (context != null) {
            Optional<Class<?>> testClass = context.getTestClass();
            if (testClass.isPresent()) {
                Class<?> tClass = testClass.get();
                count = getCountForClass(count, tClass);
                while (tClass.isAnnotationPresent(Nested.class)) {
                    try {
                        String tClassName = tClass.getName();
                        String parentClass = tClassName.substring(0, tClassName.lastIndexOf('$'));
                        log.trace("{} is Nested, adding count from parent class {}", tClassName, parentClass);
                        tClass = tClass.getClassLoader().loadClass(parentClass);
                        count += getCountForClass(count, tClass);
                    } catch (ClassNotFoundException e) {
                        log.trace("Error while loading parent class", e);
                    }
                }
            }
        } else {
            // Interactive mode
            count = 1;
        }
        log.trace("Number of required Docker browser(s): {}", count);
        return count;
    }

    private int getCountForClass(int count, Class testClass) {
        Constructor<?>[] declaredConstructors = testClass.getDeclaredConstructors();
        for (Constructor<?> constructor : declaredConstructors) {
            Parameter[] parameters = constructor.getParameters();
            count += getDockerBrowsersInParams(parameters);
        }
        Method[] methods = testClass.getMethods();
        Method[] declaredMethods = testClass.getDeclaredMethods();
        Method[] allMethods = (Method[]) ArrayUtils.addAll(methods,
                declaredMethods);
        for (Method method : allMethods) {
            Parameter[] parameters = method.getParameters();
            count += getDockerBrowsersInParams(parameters);
        }
        return count;
    }

    private int getDockerBrowsersInParams(Parameter[] parameters) {
        int count = 0;
        for (Parameter param : parameters) {
            Class<?> type = param.getType();
            if (WebDriver.class.isAssignableFrom(type)
                    || SelenideDriver.class.isAssignableFrom(type)) {
                count++;
            } else if (type.isAssignableFrom(List.class)) {
                DockerBrowser dockerBrowser = param
                        .getAnnotation(DockerBrowser.class);
                if (dockerBrowser != null) {
                    count += dockerBrowser.size();
                }
            }
        }
        return count;

    }

    private String getNoVncUrl(String selenoidHost, int selenoidPort,
            String sessionId, String novncPassword)
            throws DockerException, InterruptedException {

        DockerContainer novncContainer = startNoVncContainer();
        String novncUrl = novncContainer.getContainerUrl();

        return format(
                "%svnc.html?host=%s&port=%d&path=vnc/%s&resize=scale&autoconnect=true&password=%s",
                novncUrl, selenoidHost, selenoidPort, sessionId, novncPassword);
    }

    public DockerContainer startNoVncContainer()
            throws DockerException, InterruptedException {

        DockerContainer novncContainer;
        String novncImage = getConfig().getNovncImage();

        if (containerMap.containsKey(novncImage)) {
            log.debug("noVNC container already available");
            novncContainer = containerMap.get(novncImage);

        } else {
            dockerService.pullImage(novncImage);

            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            String defaultNovncPort = getConfig().getNovncPort();
            portBindings.put(defaultNovncPort,
                    asList(randomPort(ALL_IPV4_ADDRESSES)));

            String network = getConfig().getDockerNetwork();
            novncContainer = DockerContainer.dockerBuilder(novncImage)
                    .portBindings(portBindings).network(network).build();
            String containerId = dockerService.startContainer(novncContainer);
            String novncHost = dockerService.getHost(containerId, network);
            String novncPort = dockerService.getBindPort(containerId,
                    defaultNovncPort + "/tcp");
            String novncUrl = format("http://%s:%s/", novncHost, novncPort);
            novncContainer.setContainerId(containerId);
            novncContainer.setContainerUrl(novncUrl);

            containerMap.put(novncImage, novncContainer);
        }

        return novncContainer;
    }

    private String getDockerPath(File file) {
        String fileString = file.getAbsolutePath();
        if (fileString.contains(":")) { // Windows
            fileString = toLowerCase(fileString.charAt(0))
                    + fileString.substring(1);
            fileString = fileString.replace("\\\\", "/");
            fileString = fileString.replace(":", "");
            fileString = "/" + fileString;
        }
        log.trace("The path of file {} in Docker format is {}", file,
                fileString);
        return fileString;
    }

    private void waitForRecording() throws IOException {
        if (filesInVideoFolder != null) {
            List<File> newFilesInVideoFolder = asList(
                    hostVideoFolder.listFiles());
            Iterator<?> iterator = disjunction(filesInVideoFolder,
                    newFilesInVideoFolder).iterator();
            while (iterator.hasNext()) {
                String filename = iterator.next().toString();
                if (filename.endsWith("mp4")) {
                    recordingFile = new File(filename);
                    break;
                }
            }
        }

        if (recordingFile != null) {
            int dockerWaitTimeoutSec = dockerService.getDockerWaitTimeoutSec();
            int dockerPollTimeMs = dockerService.getDockerPollTimeMs();
            long timeoutMs = currentTimeMillis()
                    + SECONDS.toMillis(dockerWaitTimeoutSec);

            log.debug("Waiting for recording to be available");
            while (!recordingFile.exists()) {
                if (currentTimeMillis() > timeoutMs) {
                    log.warn("Timeout of {} seconds waiting for file {}",
                            dockerWaitTimeoutSec, recordingFile);
                    break;
                }
                log.trace("Recording {} not present ... waiting {} ms",
                        recordingFile, dockerPollTimeMs);
                try {
                    sleep(dockerPollTimeMs);
                } catch (InterruptedException e) {
                    log.warn(
                            "Interrupted Exception while waiting for container",
                            e);
                    currentThread().interrupt();
                }
            }

            String newRecordingName = name + ".mp4";
            log.trace("Renaming {} to {}", recordingFile, newRecordingName);
            move(recordingFile.toPath(),
                    recordingFile.toPath().resolveSibling(newRecordingName),
                    REPLACE_EXISTING);
            recordingFile = hostVideoFolder.toPath().resolve(newRecordingName).toFile();
        }
    }

    public Map<String, DockerContainer> getContainerMap() {
        return containerMap;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Config getConfig() {
        return config;
    }

    public URL getHubUrl() {
        return hubUrl;
    }

    public class Devices {
        String template;
        String device;

        public Devices(String device, String template) {
            this.device = device;
            this.template = template;
        }

    }

}
