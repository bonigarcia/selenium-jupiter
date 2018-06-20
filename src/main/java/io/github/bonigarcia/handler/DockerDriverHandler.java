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
package io.github.bonigarcia.handler;

import static com.spotify.docker.client.messages.PortBinding.randomPort;
import static io.github.bonigarcia.BrowserType.ANDROID;
import static io.github.bonigarcia.BrowserType.OPERA;
import static io.github.bonigarcia.SeleniumJupiter.config;
import static io.github.bonigarcia.SurefireReports.getOutputFolder;
import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.file.Files.move;
import static java.nio.file.Files.write;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.PortBinding;

import io.appium.java_client.android.AndroidDriver;
import io.github.bonigarcia.AnnotationsReader;
import io.github.bonigarcia.BrowserType;
import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.DockerContainer;
import io.github.bonigarcia.DockerContainer.DockerBuilder;
import io.github.bonigarcia.DockerService;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.SelenoidConfig;

/**
 * Resolver for DockerDriver's.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class DockerDriverHandler {

    static final String ALL_IPV4_ADDRESSES = "0.0.0.0";

    final Logger log = getLogger(lookup().lookupClass());

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
    boolean recording = config().isRecording();
    String selenoidImage = config().getSelenoidImage();
    String novncImage = config().getNovncImage();
    String androidNoVncUrl;

    public DockerDriverHandler() throws DockerCertificateException {
        this.selenoidConfig = new SelenoidConfig();
        this.dockerService = new DockerService();
        this.containerMap = new LinkedHashMap<>();
    }

    public DockerDriverHandler(ExtensionContext context, Parameter parameter,
            Optional<Object> testInstance, AnnotationsReader annotationsReader,
            Map<String, DockerContainer> containerMap,
            DockerService dockerService, SelenoidConfig selenoidConfig) {
        this.context = context;
        this.parameter = parameter;
        this.testInstance = testInstance;
        this.annotationsReader = annotationsReader;
        this.containerMap = containerMap;
        this.dockerService = dockerService;
        this.selenoidConfig = selenoidConfig;
    }

    public WebDriver resolve(DockerBrowser dockerBrowser) {
        BrowserType browser = dockerBrowser.type();
        String version = dockerBrowser.version();
        String browserName = dockerBrowser.browserName();
        String deviceName = dockerBrowser.deviceName();

        return resolve(browser, version, browserName, deviceName);
    }

    public WebDriver resolve(BrowserType browser, String version,
            String browserName, String deviceName) {
        try {
            if (recording) {
                hostVideoFolder = new File(getOutputFolder(context));
            }

            WebDriver webdriver;
            if (browser == ANDROID) {
                webdriver = getDriverForAndroid(browser, version, browserName,
                        deviceName);
            } else {
                if (selenoidConfig == null) {
                    selenoidConfig = new SelenoidConfig();
                }
                webdriver = getDriverForBrowser(browser, version);
            }
            return webdriver;

        } catch (Exception e) {
            log.error("Exception resolving {} ({} {})", parameter, browser,
                    version, e);
            throw new SeleniumJupiterException(e);
        }
    }

    private WebDriver getDriverForBrowser(BrowserType browser, String version)
            throws IllegalAccessException, IOException, DockerException,
            InterruptedException {
        boolean enableVnc = config().isVnc();
        DesiredCapabilities capabilities = getCapabilities(browser, enableVnc);

        String imageVersion;
        String versionFromLabel = version;
        if (version != null && !version.isEmpty()
                && !version.equalsIgnoreCase("latest")) {
            if (version.startsWith("latest-")) {
                versionFromLabel = selenoidConfig.getVersionFromLabel(browser,
                        version);
            }
            imageVersion = selenoidConfig.getImageVersion(browser,
                    versionFromLabel);
            capabilities.setCapability("version", imageVersion);
        } else {
            imageVersion = selenoidConfig.getDefaultBrowser(browser);
        }

        String seleniumServerUrl = config().getSeleniumServerUrl();
        boolean seleniumServerUrlAvailable = seleniumServerUrl != null
                && !seleniumServerUrl.isEmpty();
        String hubUrl = seleniumServerUrlAvailable ? seleniumServerUrl
                : startDockerBrowser(browser, versionFromLabel);

        log.trace("Using Selenium Server at {}", hubUrl);
        WebDriver webdriver = new RemoteWebDriver(new URL(hubUrl),
                capabilities);

        SessionId sessionId = ((RemoteWebDriver) webdriver).getSessionId();
        updateName(browser, imageVersion, webdriver);

        if (enableVnc && !seleniumServerUrlAvailable) {
            URL selenoidHubUrl = new URL(hubUrl);
            String selenoidHost = selenoidHubUrl.getHost();
            int selenoidPort = selenoidHubUrl.getPort();

            String novncUrl = getNoVncUrl(selenoidHost, selenoidPort,
                    sessionId.toString(), config().getSelenoidVncPassword());
            logSessionId(sessionId);
            logNoVncUrl(novncUrl);

            String vncExport = config().getVncExport();
            log.trace("Exporting VNC URL as Java property {}", vncExport);
            System.setProperty(vncExport, novncUrl);

            if (config().isVncRedirectHtmlPage()) {
                String outputFolder = getOutputFolder(context);
                String vncHtmlPage = format("<!DOCTYPE html>\n" + "<html>\n"
                        + "<head>\n"
                        + "<meta http-equiv=\"refresh\" content=\"0; url=%s\">\n"
                        + "</head>\n" + "<body>\n" + "</body>\n" + "</html>",
                        novncUrl);
                write(Paths.get(outputFolder, name + ".html"),
                        vncHtmlPage.getBytes());
            }
        }

        if (recording) {
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

    private WebDriver getDriverForAndroid(BrowserType browser, String version,
            String browserName, String deviceName)
            throws DockerException, InterruptedException, IOException {
        browser.init();

        DesiredCapabilities capabilities = browser.getCapabilities();
        String browserNameCapability = browserName != null
                && !browserName.isEmpty() ? browserName
                        : config().getAndroidBrowserName();
        String deviceNameCapability = deviceName != null
                && !deviceName.isEmpty() ? deviceName
                        : config().getAndroidDeviceName();
        capabilities.setCapability("browserName", browserNameCapability);
        capabilities.setCapability("deviceName", deviceNameCapability);

        String appiumUrl = startAndroidBrowser(version, deviceNameCapability);
        AndroidDriver<WebElement> androidDriver = null;

        log.info("Appium URL in Android device: {}", appiumUrl);
        log.info("Android device name: {} -- Browser in Android device: {}",
                deviceNameCapability, browserNameCapability);
        log.info(
                "Waiting for Android device ... this might take long, please wait (retries each 5 seconds)");

        int androidDeviceTimeoutSec = config().getAndroidDeviceTimeoutSec();
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
                String errorMessage = e.getMessage();
                int i = errorMessage.indexOf('\n');
                if (i != -1) {
                    errorMessage = errorMessage.substring(0, i);
                }
                log.debug("Android device not ready: {}", errorMessage);
                sleep(5000);
            }
        } while (androidDriver == null);
        log.info("Android device ready {}", androidDriver);

        if (config().isVnc()) {
            logSessionId(androidDriver.getSessionId());
            logNoVncUrl(androidNoVncUrl);
        }
        return androidDriver;
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

    private DesiredCapabilities getCapabilities(BrowserType browser,
            boolean enableVnc) throws IllegalAccessException, IOException {
        DesiredCapabilities capabilities = browser.getCapabilities();
        if (enableVnc) {
            capabilities.setCapability("enableVNC", true);
            capabilities.setCapability("screenResolution",
                    config().getVncScreenResolution());
        }

        if (recording) {
            capabilities.setCapability("enableVideo", true);
            capabilities.setCapability("videoScreenSize",
                    config().getRecordingVideoScreenSize());
            capabilities.setCapability("videoFrameRate",
                    config().getRecordingVideoFrameRate());
        }

        Optional<Capabilities> optionalCapabilities = annotationsReader != null
                ? annotationsReader.getCapabilities(parameter, testInstance)
                : Optional.of(new DesiredCapabilities());
        MutableCapabilities options = browser.getDriverHandler()
                .getOptions(parameter, testInstance);

        // Due to bug in operablink the binary path must be set
        if (browser == OPERA) {
            ((OperaOptions) options).setBinary("/usr/bin/opera");
        }

        if (optionalCapabilities.isPresent()) {
            options.merge(optionalCapabilities.get());
        }
        capabilities.setCapability(browser.getOptionsKey(), options);
        log.trace("Using {}", capabilities);
        return capabilities;
    }

    public String getName() {
        return name;
    }

    public void cleanup() {
        try {
            // Wait for recordings
            if (recording) {
                waitForRecording();
            }
            // Clear VNC URL
            String vncExport = config().getVncExport();
            if (config().isVnc() && System.getProperty(vncExport) != null) {
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
                        log.warn("Exception cleaning Docker containers {}",
                                e.getMessage());
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

    private String startAndroidBrowser(String version, String deviceName)
            throws DockerException, InterruptedException, IOException {

        if (version == null || version.isEmpty()) {
            version = config().getAndroidDefaultVersion();
        }

        String androidImage;
        String apiLevel;
        switch (version) {
        case "5.0.1":
            androidImage = IS_OS_LINUX ? config().getAndroidImageApi21Linux()
                    : config().getAndroidImageApi21OsxWin();
            apiLevel = "21";
            break;
        case "5.1.1":
            androidImage = IS_OS_LINUX ? config().getAndroidImageApi22Linux()
                    : config().getAndroidImageApi22OsxWin();
            apiLevel = "22";
            break;
        case "6.0":
            androidImage = IS_OS_LINUX ? config().getAndroidImageApi23Linux()
                    : config().getAndroidImageApi23OsxWin();
            apiLevel = "23";
            break;
        case "7.0":
            androidImage = IS_OS_LINUX ? config().getAndroidImageApi24Linux()
                    : config().getAndroidImageApi24OsxWin();
            apiLevel = "24";
            break;
        case "7.1.1":
            androidImage = IS_OS_LINUX ? config().getAndroidImageApi25Linux()
                    : config().getAndroidImageApi25OsxWin();
            apiLevel = "25";
            break;
        default:
            throw new SeleniumJupiterException(
                    "Version " + version + " not valid for Android devices");
        }

        log.info("Using Android version {} (API level {})", version, apiLevel);
        dockerService.pullImage(androidImage);

        DockerContainer androidContainer = startAndroidContainer(androidImage,
                deviceName);
        return androidContainer.getContainerUrl();

    }

    private String startDockerBrowser(BrowserType browser, String version)
            throws DockerException, InterruptedException, IOException {

        String browserImage;
        if (version == null || version.isEmpty()
                || version.equalsIgnoreCase("latest")) {
            log.info("Using {} version {} (latest)", browser,
                    selenoidConfig.getDefaultBrowser(browser));
            browserImage = selenoidConfig.getLatestImage(browser);
        } else {
            log.info("Using {} version {}", browser, version);
            browserImage = selenoidConfig.getImageFromVersion(browser, version);
        }
        dockerService.pullImage(browserImage);

        DockerContainer selenoidContainer = startSelenoidContainer();
        return selenoidContainer.getContainerUrl();
    }

    public DockerContainer startSelenoidContainer()
            throws DockerException, InterruptedException, IOException {

        DockerContainer selenoidContainer;
        if (containerMap.containsKey(selenoidImage)) {
            log.trace("Selenoid container already available");
            selenoidContainer = containerMap.get(selenoidImage);
        } else {
            // Pull images
            dockerService.pullImageIfNecessary(selenoidImage);
            String recordingImage = config().getRecordingImage();
            if (recording) {
                dockerService.pullImageIfNecessary(recordingImage);
            }

            // portBindings
            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            String defaultSelenoidPort = config().getSelenoidPort();
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
            String internalBrowserPort = config().getSelenoidPort();
            String browsersJson = selenoidConfig.getBrowsersJsonAsString();
            String browserTimeout = config().getBrowserSessionTimeoutDuration();
            String network = config().getDockerNetwork();

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
            List<String> envs = new ArrayList<>();
            envs.add("DOCKER_API_VERSION=" + config().getDockerApiVersion());
            envs.add("TZ=" + config().getDockerTimeZone());

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
            String containerId = dockerService
                    .startContainer(selenoidContainer);
            String selenoidHost = dockerService.getHost(containerId, network);
            String selenoidPort = dockerService.getBindPort(containerId,
                    internalSelenoidPort + "/tcp");
            String selenoidUrl = format("http://%s:%s/wd/hub", selenoidHost,
                    selenoidPort);
            selenoidContainer.setContainerId(containerId);
            selenoidContainer.setContainerUrl(selenoidUrl);

            containerMap.put(selenoidImage, selenoidContainer);
        }
        return selenoidContainer;
    }

    public DockerContainer startAndroidContainer(String androidImage,
            String deviceName)
            throws DockerException, InterruptedException, IOException {

        DockerContainer androidContainer;
        if (containerMap.containsKey(androidImage)) {
            log.trace("Android container already available");
            androidContainer = containerMap.get(androidImage);
        } else {
            // Pull image
            dockerService.pullImageIfNecessary(androidImage);

            // portBindings
            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            String internalAppiumPort = config().getAndroidAppiumPort();
            portBindings.put(internalAppiumPort,
                    asList(randomPort(ALL_IPV4_ADDRESSES)));
            String internalNoVncPort = config().getAndroidNoVncPort();
            portBindings.put(internalNoVncPort,
                    asList(randomPort(ALL_IPV4_ADDRESSES)));

            // binds
            List<String> binds = new ArrayList<>();
            if (recording) {
                binds.add(getDockerPath(hostVideoFolder) + ":/tmp/video");
            }

            // network
            String network = config().getDockerNetwork();

            // envs
            List<String> envs = new ArrayList<>();
            envs.add("DEVICE=" + deviceName);
            envs.add("APPIUM=True");
            if (recording) {
                envs.add("AUTO_RECORD=True");
            }

            // Build container
            DockerBuilder dockerBuilder = DockerContainer
                    .dockerBuilder(androidImage).portBindings(portBindings)
                    .binds(binds).envs(envs).network(network).privileged();

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
            androidNoVncUrl = format("http://%s:%s/vnc.html?autoconnect=true",
                    androidHost, androidNoVncPort);

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
            throws DockerException, InterruptedException, IOException {

        DockerContainer novncContainer = startNoVncContainer();
        String novncUrl = novncContainer.getContainerUrl();

        return format(novncUrl
                + "vnc.html?host=%s&port=%d&path=vnc/%s&resize=scale&autoconnect=true&password=%s",
                selenoidHost, selenoidPort, sessionId, novncPassword);
    }

    public DockerContainer startNoVncContainer()
            throws DockerException, InterruptedException, IOException {

        DockerContainer novncContainer;
        if (containerMap.containsKey(novncImage)) {
            log.debug("noVNC container already available");
            novncContainer = containerMap.get(novncImage);

        } else {
            dockerService.pullImageIfNecessary(novncImage);

            Map<String, List<PortBinding>> portBindings = new HashMap<>();
            String defaultNovncPort = config().getNovncPort();
            portBindings.put(defaultNovncPort,
                    asList(randomPort(ALL_IPV4_ADDRESSES)));

            String network = config().getDockerNetwork();
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
        int dockerWaitTimeoutSec = dockerService.getDockerWaitTimeoutSec();
        int dockerPollTimeMs = dockerService.getDockerPollTimeMs();
        long timeoutMs = currentTimeMillis()
                + SECONDS.toMillis(dockerWaitTimeoutSec);

        log.debug("Waiting for recording {} to be available", recordingFile);
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
                log.warn("Interrupted Exception while waiting for container",
                        e);
                currentThread().interrupt();
            }
        }

        log.trace("Renaming {} to {}.mp4", recordingFile, name);
        move(recordingFile.toPath(),
                recordingFile.toPath().resolveSibling(name + ".mp4"),
                REPLACE_EXISTING);
    }

    public Map<String, DockerContainer> getContainerMap() {
        return containerMap;
    }

    public void setIndex(String index) {
        this.index = index;
    }

}
