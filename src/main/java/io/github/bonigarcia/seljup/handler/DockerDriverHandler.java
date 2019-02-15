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

import static com.spotify.docker.client.messages.PortBinding.randomPort;
import static io.github.bonigarcia.seljup.BrowserType.ANDROID;
import static io.github.bonigarcia.seljup.BrowserType.OPERA;
import static io.github.bonigarcia.seljup.CloudType.GENYMOTION_PAAS;
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
import static java.nio.file.Files.move;
import static java.nio.file.Files.write;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.slf4j.Logger;

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
    static final String BROWSER = "browser";
    static final String CHROME = "chrome";

    final Logger log = getLogger(lookup().lookupClass());

    Config config;
    DockerService dockerService;
    SelenoidConfig selenoidConfig;
    Map<String, DockerContainer> containerMap;
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

    boolean androidLogging = getConfig().isAndroidLogging();
    File hostAndroidLogsFolder;

    public DockerDriverHandler(Config config, BrowserInstance browserInstance,
            String version, InternalPreferences preferences) {
        this.config = config;
        this.selenoidConfig = new SelenoidConfig(config, browserInstance,
                version);
        this.dockerService = new DockerService(config, preferences);
        this.containerMap = new LinkedHashMap<>();
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
        this.dockerService = dockerService;
        this.config = config;
        this.selenoidConfig = new SelenoidConfig(getConfig(), browserInstance,
                version);
    }

    public WebDriver resolve(DockerBrowser dockerBrowser) {
        BrowserType browserType = dockerBrowser.type();
        CloudType cloudType = dockerBrowser.cloud();
        BrowserInstance browserInstance = new BrowserInstance(config,
                annotationsReader, browserType, cloudType,
                Optional.ofNullable(dockerBrowser.browserName()));
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
                dockerService.updateDockerClient(url);
            }
            if (getConfig().isRecording()) {
                hostVideoFolder = new File(getOutputFolder(context,
                        getConfig().getOutputFolder()));
            }

            if (androidLogging) {
                String dateTime = DateTimeFormatter.ofPattern("uuuu-MM-dd--HH-mm-ss").format(LocalDateTime.now());
                String logsFolder = getConfig().getAndroidLogsFolder();
                Path path = Paths.get(getOutputFolder(context, getConfig().getOutputFolder()), logsFolder, dateTime);
                try {
                    Files.createDirectories(path);
                    hostAndroidLogsFolder = path.toFile();
                    log.info("Android logs will be stored in " + path.toAbsolutePath());
                } catch (IOException e) {
                    log.error("Failed to create directories for android logs " + path.toAbsolutePath(), e);
                    androidLogging = false;
                }
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
            capabilities.setCapability("version", imageVersion);
        } else {
            imageVersion = selenoidConfig.getDefaultBrowser(browserType);
        }

        String seleniumServerUrl = getConfig().getSeleniumServerUrl();
        boolean seleniumServerUrlAvailable = seleniumServerUrl != null
                && !seleniumServerUrl.isEmpty();
        hubUrl = new URL(seleniumServerUrlAvailable ? seleniumServerUrl
                : startDockerBrowser(browserInstance, versionFromLabel));

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

        if (getConfig().isRecording()) {
            recordingFile = new File(hostVideoFolder, sessionId + ".mp4");
        }
        return webdriver;
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
        if (getConfig().isRecording()) {
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

        log.info("Appium URL in Android device: {}", appiumUrl);
        log.info("Android device name: {} -- Browser: {}", deviceNameCapability,
                browserName);
        log.info(
                "Waiting for Android device ... this might take long, please wait (retries each 5 seconds)");

        AndroidDriver<WebElement> androidDriver = null;
        int androidDeviceTimeoutSec = getConfig().getAndroidDeviceTimeoutSec();
        long endTimeMillis = currentTimeMillis()
                + androidDeviceTimeoutSec * 1000;
        do {
            try {
                androidDriver = new AndroidDriver<>(new URL(appiumUrl),
                        capabilities);
            } catch (Exception e) {
                if (currentTimeMillis() > endTimeMillis) {
                    throw new SeleniumJupiterException("Timeout ("
                            + androidDeviceTimeoutSec
                            + " seconds) waiting for Android device in Docker");
                }
                String errorMessage = getErrorMessage(e);
                log.debug("Android device not ready: {}", errorMessage);
                if (errorMessage.contains("Could not find package")) {
                    throw new SeleniumJupiterException(errorMessage);
                }
                sleep(5000);
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

        if (getConfig().isRecording()) {
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
        if (browserInstance.getBrowserType() == OPERA) {
            ((OperaOptions) options).setBinary("/usr/bin/opera");
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
            // Wait for recordings
            if (getConfig().isRecording()) {
                waitForRecording();
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
            // Stop containers
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
            String browserVersion;
            String apiLevel;

            switch (version) {
            case "5.0.1":
            case LATEST + "-7":
                androidImage = getConfig().getAndroidImage501();
                apiLevel = "21";
                browserName = BROWSER;
                browserVersion = "37.0";
                break;
            case "5.1.1":
            case LATEST + "-6":
                androidImage = getConfig().getAndroidImage511();
                apiLevel = "22";
                browserName = BROWSER;
                browserVersion = "39.0";
                break;
            case "6.0":
            case LATEST + "-5":
                androidImage = getConfig().getAndroidImage60();
                apiLevel = "23";
                browserName = BROWSER;
                browserVersion = "44.0";
                break;
            case "7.0":
            case LATEST + "-4":
                androidImage = getConfig().getAndroidImage701();
                apiLevel = "24";
                browserName = CHROME;
                browserVersion = "51.0";
                break;
            case "7.1.1":
            case LATEST + "-3":
                androidImage = getConfig().getAndroidImage711();
                apiLevel = "25";
                browserName = CHROME;
                browserVersion = "55.0";
                break;
            case "8.0":
            case LATEST + "-2":
                androidImage = getConfig().getAndroidImage80();
                apiLevel = "26";
                browserName = CHROME;
                browserVersion = "58.0";
                break;
            case "8.1":
            case LATEST + "-1":
                androidImage = getConfig().getAndroidImage81();
                apiLevel = "27";
                browserName = CHROME;
                browserVersion = "61.0";
                break;
            case "9.0":
            case LATEST:
                androidImage = getConfig().getAndroidImage90();
                apiLevel = "28";
                browserName = CHROME;
                browserVersion = "66.0";
                break;
            default:
                throw new SeleniumJupiterException("Version " + version
                        + " not valid for Android devices");
            }
            log.info("Starting {} {} in Android {} (API level {})", browserName,
                    browserVersion, version, apiLevel);

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
        dockerService.pullImage(browserImage);

        DockerContainer selenoidContainer = startSelenoidContainer();
        return selenoidContainer.getContainerUrl();
    }

    public DockerContainer startSelenoidContainer()
            throws DockerException, InterruptedException {

        DockerContainer selenoidContainer;
        String selenoidImage = getConfig().getSelenoidImage();
        boolean recording = getConfig().isRecording();

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

            List<String> cmd = asList("sh", "-c",
                    "mkdir -p /etc/selenoid/; echo '" + browsersJson
                            + "' > /etc/selenoid/browsers.json; /usr/bin/selenoid"
                            + " -listen :" + internalBrowserPort
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
            boolean recording = getConfig().isRecording();
            List<String> binds = new ArrayList<>();
            if (recording) {
                binds.add(getDockerPath(hostVideoFolder) + ":/tmp/video");
            }
            if(androidLogging) {
                binds.add(getDockerPath(hostAndroidLogsFolder) + ":/var/log/supervisor");
            }
            if (cloudType == GENYMOTION_PAAS) {
                binds.add(getConfig().getAndroidGenymotionAwsJson()
                        + ":/root/tmp");
            }

            // network
            String network = getConfig().getDockerNetwork();
            String screenWidth = getConfig().getAndroidScreenWidth();
            String screenHeigth = getConfig().getAndroidScreenHeigth();
            String screenDepth = getConfig().getAndroidScreenDepth();

            // envs
            List<String> envs = new ArrayList<>();
            envs.add("DEVICE=" + deviceName);
            envs.add("SCREEN_WIDTH=" + screenWidth);
            envs.add("SCREEN_HEIGHT=" + screenHeigth);
            envs.add("SCREEN_DEPTH=" + screenDepth);
            envs.add("APPIUM=true");
            List<String> proxyEnvVars = asList("HTTP_PROXY", "HTTPS_PROXY",
                    "NO_PROXY", "http_proxy", "https_proxy", "no_proxy");
            proxyEnvVars.stream().filter(envName -> isNotBlank(getenv(envName)))
                    .forEach(envName -> envs
                            .add(envName + "=" + getenv(envName)));
            if (recording) {
                envs.add("AUTO_RECORD=true");
            } else {
                envs.add("AUTO_RECORD=false");
            }

            if (cloudType == GENYMOTION_SAAS) {
                envs.add("TYPE=SaaS");
                envs.add("USER=" + getConfig().getAndroidGenymotionUser());
                envs.add("PASS=" + getConfig().getAndroidGenymotionPassword());
                envs.add(
                        "LICENSE=" + getConfig().getAndroidGenymotionLicense());
            } else if (cloudType == GENYMOTION_PAAS) {
                envs.add("TYPE=aws");
            }

            // Build container
            DockerBuilder dockerBuilder = DockerContainer
                    .dockerBuilder(androidImage).portBindings(portBindings)
                    .binds(binds).envs(envs).network(network).privileged();

            if (cloudType == GENYMOTION_SAAS) {
                Devices[] devices = new Devices[1];
                devices[0] = new Devices(deviceName,
                        getConfig().getAndroidGenymotionTemplate());
                String deviceJson = new GsonBuilder().disableHtmlEscaping()
                        .create().toJson(devices);

                log.trace("Devices.json = {}", deviceJson);
                List<String> cmd = asList("sh", "-c", "mkdir /root/tmp; echo '"
                        + deviceJson
                        + "' > /root/tmp/devices.json; ./geny_start.sh");
                dockerBuilder.cmd(cmd);
            }

            androidContainer = dockerBuilder.build();
            String containerId = dockerService.startContainer(androidContainer);

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

    private int getDockerBrowserCount() {
        int count = 0;
        if (context != null) {
            Optional<Class<?>> testClass = context.getTestClass();
            if (testClass.isPresent()) {
                Constructor<?>[] declaredConstructors = testClass.get()
                        .getDeclaredConstructors();
                for (Constructor<?> constructor : declaredConstructors) {
                    Parameter[] parameters = constructor.getParameters();
                    count += getDockerBrowsersInParams(parameters);
                }
                Method[] declaredMethods = testClass.get().getDeclaredMethods();
                for (Method method : declaredMethods) {
                    Parameter[] parameters = method.getParameters();
                    count += getDockerBrowsersInParams(parameters);
                }
            }
        } else {
            // Interactive mode
            count = 1;
        }
        log.trace("Number of required Docker browser(s): {}", count);
        return count;
    }

    private int getDockerBrowsersInParams(Parameter[] parameters) {
        int count = 0;
        for (Parameter param : parameters) {
            Class<?> type = param.getType();
            if (WebDriver.class.isAssignableFrom(type)) {
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

        return format(novncUrl
                + "vnc.html?host=%s&port=%d&path=vnc/%s&resize=scale&autoconnect=true&password=%s",
                selenoidHost, selenoidPort, sessionId, novncPassword);
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
            fileString = fileString.replaceAll("\\\\", "/");
            fileString = fileString.replaceAll(":", "");
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
            if (iterator.hasNext()) {
                String filename = iterator.next().toString();
                recordingFile = new File(filename);
            }
        }

        if (recordingFile != null) {
            int dockerWaitTimeoutSec = dockerService.getDockerWaitTimeoutSec();
            int dockerPollTimeMs = dockerService.getDockerPollTimeMs();
            long timeoutMs = currentTimeMillis()
                    + SECONDS.toMillis(dockerWaitTimeoutSec);

            log.debug("Waiting for recording {} to be available",
                    recordingFile);
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

            log.trace("Renaming {} to {}.mp4", recordingFile, name);
            move(recordingFile.toPath(),
                    recordingFile.toPath().resolveSibling(name + ".mp4"),
                    REPLACE_EXISTING);
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
