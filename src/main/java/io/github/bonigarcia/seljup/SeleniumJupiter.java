/*
 * (C) Copyright 2017 Boni Garcia (https://bonigarcia.github.io/)
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

import static io.github.bonigarcia.seljup.BrowserType.CHROME_MOBILE;
import static io.github.bonigarcia.wdm.WebDriverManager.isOnline;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import com.google.gson.Gson;

import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import io.github.bonigarcia.seljup.config.Config;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;

/**
 * JUnit 5 extension for Selenium WebDriver tests.
 *
 * @author Boni Garcia
 * @since 1.0.0
 */
public class SeleniumJupiter implements ParameterResolver,
        AfterTestExecutionCallback, AfterEachCallback, AfterAllCallback,
        TestTemplateInvocationContextProvider, ExecutionCondition {

    final Logger log = getLogger(lookup().lookupClass());

    static final String CLASSPATH_PREFIX = "classpath:";
    static final String DEVTOOLS_CLASS = "org.openqa.selenium.devtools.DevTools";
    static final String HTMLUNIT_DRIVER_CLASS = "org.openqa.selenium.htmlunit.HtmlUnitDriver";
    static final String APPIUM_DRIVER_CLASS = "io.appium.java_client.AppiumDriver";

    static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
            .enabled("Test enabled");

    Config config;
    Map<String, List<WebDriverManager>> wdmMap;
    List<DevTools> devToolsList;
    AnnotationsReader annotationsReader;
    List<List<Browser>> browserListList;
    Map<String, List<Browser>> browserListMap;
    OutputHandler outputHandler;
    URL urlFromAnnotation;
    SelenideHandler selenideHandler;

    public SeleniumJupiter() {
        config = new Config();
        wdmMap = Collections.synchronizedMap(new LinkedHashMap<>());
        annotationsReader = new AnnotationsReader();
        browserListList = new ArrayList<>();
        browserListMap = new ConcurrentHashMap<>();
        devToolsList = new ArrayList<>();
        selenideHandler = new SelenideHandler(annotationsReader);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        Parameter parameter = parameterContext.getParameter();
        Class<?> type = parameter.getType();
        Type parameterizedType = parameter.getParameterizedType();
        String parameterizedTypeName = "";
        if (ParameterizedType.class
                .isAssignableFrom(parameterizedType.getClass())) {
            parameterizedTypeName = ((ParameterizedType) parameterizedType)
                    .getActualTypeArguments()[0].getTypeName();
        }
        Optional<DockerBrowser> dockerBrowser = annotationsReader
                .getDocker(parameter);

        return (WebDriver.class.isAssignableFrom(type)
                || type.equals(DevTools.class)
                || (type.equals(List.class) && dockerBrowser.isPresent()
                        && isGeneric(parameterizedTypeName))
                || selenideHandler.isSelenide(type))
                && !isTestTemplate(extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        String contextId = getContextId(extensionContext);
        Parameter parameter = parameterContext.getParameter();
        int index = parameterContext.getIndex();
        Optional<Object> testInstance = extensionContext.getTestInstance();

        log.trace("Resolving parameter {} (contextId {}, index {})", parameter,
                contextId, index);

        Class<?> type = parameter.getType();
        switch (type.getName()) {
        // DevTools
        case DEVTOOLS_CLASS:
            return resolveDevTools(contextId, index);

        // HtmlUnit
        case HTMLUNIT_DRIVER_CLASS:
            return resolveHtmlUnit(type, extensionContext, parameter);

        // Appium
        case APPIUM_DRIVER_CLASS:
            return resolveAppium(testInstance, parameter);

        // Selenium WebDriver
        default:
            return resolveSeleniumWebDriver(extensionContext, contextId,
                    parameter, index, testInstance, type);
        }
    }

    private Object resolveSeleniumWebDriver(ExtensionContext extensionContext,
            String contextId, Parameter parameter, int index,
            Optional<Object> testInstance, Class<?> type) {
        WebDriverManager wdm = null;
        Browser browser = null;
        int browserNumber = 0;

        boolean isGeneric = isGeneric(type);
        boolean isSelenide = selenideHandler.isSelenide(type);
        Optional<DockerBrowser> dockerBrowser = annotationsReader
                .getDocker(parameter);

        Optional<URL> url = findUrl(parameter, testInstance);
        Optional<Capabilities> caps = annotationsReader
                .getCapabilities(parameter, testInstance);
        // Single session
        if (isSingleSession(extensionContext) && wdmMap.containsKey(contextId)
                && index < wdmMap.get(contextId).size()) {
            WebDriver driver = wdmMap.get(contextId).get(index).getWebDriver();
            if (driver != null) {
                log.trace("Returning driver at index {}: {}", index, driver);
                return driver;
            }
        }

        if (config.getManager() != null) { // Custom manager
            wdm = config.getManager();

        } else if (dockerBrowser.isPresent()) { // Docker
            if (dockerBrowser.get().size() > 0) {
                browserNumber = dockerBrowser.get().size();
            }
            wdm = getManagerForDocker(extensionContext, parameter,
                    dockerBrowser.get());

        } else if (url.isPresent() && caps.isPresent()) { // Remote
            wdm = getManagerForRemote(url.get(), caps.get());

        } else if (isGeneric || isSelenide) { // Template
            browser = getBrowser(contextId, index);
            wdm = getManagerForTemplate(extensionContext, parameter, browser,
                    url);

        } else { // Local
            wdm = getManagerForLocal(extensionContext, parameter, type,
                    isGeneric);
        }

        // Output folder
        outputHandler = new OutputHandler(extensionContext, getConfig(),
                parameter);
        wdm.dockerRecordingPrefix(outputHandler.getPrefix());
        wdm.dockerRecordingOutput(outputHandler.getOutputFolder());

        putManagerInMap(contextId, wdm);

        return getObjectFromWdm(wdm, browser, browserNumber, isSelenide,
                parameter, testInstance);
    }

    @SuppressWarnings("unchecked")
    private Object getObjectFromWdm(WebDriverManager wdm, Browser browser,
            int browserNumber, boolean isSelenide, Parameter parameter,
            Optional<Object> testInstance) {
        Object object = null;
        if (!isSelenide || !selenideHandler.useCustomSelenideConfig(parameter,
                testInstance)) {
            object = browserNumber == 0 ? wdm.create()
                    : wdm.create(browserNumber);
        }
        if (object != null && (isSelenide
                || (browser != null && browser.isInSelenide()))) {
            if (browserNumber == 0) {
                object = selenideHandler.createSelenideDriver(
                        (WebDriver) object, parameter, testInstance);
            } else {
                object = ((List<WebDriver>) object).stream()
                        .map(driver -> selenideHandler.createSelenideDriver(
                                driver, parameter, testInstance))
                        .collect(Collectors.toList());
            }
        }
        return object;
    }

    private Optional<URL> findUrl(Parameter parameter,
            Optional<Object> testInstance) {
        Optional<URL> url = annotationsReader.getUrl(parameter, testInstance,
                config.getSeleniumServerUrl());
        if (!url.isPresent() && urlFromAnnotation != null) {
            url = Optional.of(urlFromAnnotation);
        }
        return url;
    }

    private Object resolveDevTools(String contextId, int index) {
        if (wdmMap != null && wdmMap.get(contextId) != null
                && wdmMap.get(contextId).size() >= index) {
            WebDriver driver = wdmMap.get(contextId).get(index - 1)
                    .getWebDriver();
            log.debug("Opening DevTools for {}", driver);
            DevTools devTools = ((HasDevTools) driver).getDevTools();
            devTools.createSessionIfThereIsNotOne();
            devToolsList.add(devTools);
            return devTools;
        } else {
            throw new SeleniumJupiterException(
                    "Incorrect position of DevTool arguments"
                            + " (it should be declared after a ChromiumDriver parameter)");
        }
    }

    private Object resolveHtmlUnit(Class<?> type,
            ExtensionContext extensionContext, Parameter parameter) {
        WebDriver driver = null;
        try {
            Optional<Capabilities> capabilities = getCapabilities(
                    extensionContext, parameter, Optional.empty(),
                    Optional.empty());

            if (capabilities.isPresent()) {
                driver = (WebDriver) type
                        .getDeclaredConstructor(Capabilities.class)
                        .newInstance(capabilities.get());
            } else {
                driver = (WebDriver) type.getDeclaredConstructor()
                        .newInstance();
            }
        } catch (Exception e) {
            log.warn("Exception trying to create HtmlUnit instance", e);
        }
        return driver;
    }

    private Object resolveAppium(Optional<Object> testInstance,
            Parameter parameter) {
        Object driver = null;
        try {
            Optional<URL> url = findUrl(parameter, testInstance);
            Optional<Capabilities> caps = annotationsReader
                    .getCapabilities(parameter, testInstance);

            if (url.isPresent() && caps.isPresent()) {
                driver = Class.forName(APPIUM_DRIVER_CLASS)
                        .getDeclaredConstructor(URL.class, Capabilities.class)
                        .newInstance(url.get(), caps.get());
            }
        } catch (Exception e) {
            log.warn("Exception creating instance of AppiumDriver", e);
        }
        return driver;
    }

    private String getContextId(ExtensionContext extensionContext) {
        Optional<ExtensionContext> parent = extensionContext.getParent();
        return parent.isPresent()
                && extensionContext.getClass().getCanonicalName().equals(
                        "org.junit.jupiter.engine.descriptor.MethodExtensionContext")
                                ? parent.get().getUniqueId()
                                : extensionContext.getUniqueId();
    }

    private WebDriverManager getManagerForRemote(URL url, Capabilities caps) {
        WebDriverManager wdm;
        wdm = WebDriverManager.getInstance().remoteAddress(url.toString())
                .capabilities(caps);
        return wdm;
    }

    @SuppressWarnings("unchecked")
    private WebDriverManager getManagerForLocal(
            ExtensionContext extensionContext, Parameter parameter,
            Class<?> type, boolean isGeneric) {
        WebDriverManager wdm;
        if (type == List.class) {
            throw new SeleniumJupiterException(
                    "List<WebDriver> must be used together with @DockerBrowser");
        }
        if (isGeneric) {
            wdm = WebDriverManager.getInstance();
        } else {
            Class<? extends WebDriver> webdriverClass = (Class<? extends WebDriver>) type;
            wdm = WebDriverManager.getInstance(webdriverClass);
        }

        Optional<Capabilities> capabilities = getCapabilities(extensionContext,
                parameter, Optional.empty(), Optional.empty());
        if (capabilities.isPresent()) {
            wdm.capabilities(capabilities.get());
        }
        return wdm;
    }

    private WebDriverManager getManagerForDocker(
            ExtensionContext extensionContext, Parameter parameter,
            DockerBrowser dockerBrowser) {
        WebDriverManager wdm;
        String browserVersion = dockerBrowser.version();
        BrowserType browserType = dockerBrowser.type();
        wdm = WebDriverManager.getInstance(browserType.toBrowserName())
                .browserVersion(browserVersion).browserInDocker();
        if (browserType == CHROME_MOBILE) {
            wdm.browserInDockerAndroid();
        }
        if (dockerBrowser.recording() || config.isRecording()
                || config.isRecordingWhenFailure()) {
            wdm.enableRecording();
        }
        if (dockerBrowser.vnc() || config.isVnc()) {
            wdm.enableVnc();
        }
        if (dockerBrowser.volumes().length > 0) {
            wdm.dockerVolumes(dockerBrowser.volumes());
        }
        if (!dockerBrowser.lang().isEmpty()) {
            wdm.dockerLang(dockerBrowser.lang());
        }
        if (!dockerBrowser.timezone().isEmpty()) {
            wdm.dockerTimezone(dockerBrowser.timezone());
        }
        Optional<Capabilities> capabilities = getCapabilities(extensionContext,
                parameter, Optional.of(browserType), Optional.empty());
        if (capabilities.isPresent()) {
            wdm.capabilities(capabilities.get());
        }
        return wdm;
    }

    private WebDriverManager getManagerForTemplate(
            ExtensionContext extensionContext, Parameter parameter,
            Browser browser, Optional<URL> url) {
        WebDriverManager wdm;
        Optional<BrowserType> browserType = Optional.empty();
        Optional<Browser> opBrowser = Optional.empty();
        if (browser != null) {
            opBrowser = Optional.of(browser);
            browserType = Optional.of(browser.toBrowserType());
            wdm = WebDriverManager
                    .getInstance(browserType.get().toBrowserName())
                    .browserVersion(browser.getVersion())
                    .remoteAddress(browser.getRemoteUrl());
            if (url.isPresent()) {
                wdm.remoteAddress(url.get().toString());
            }
            if (browser.isDockerBrowser()) {
                wdm.browserInDocker();
            }
            if (browser.isAndroidBrowser()) {
                wdm.browserInDockerAndroid();
            }
            if (config.isRecording() || config.isRecordingWhenFailure()) {
                wdm.enableRecording();
            }
            if (config.isVnc()) {
                wdm.enableVnc();
            }

        } else {
            wdm = WebDriverManager.getInstance();
        }

        Optional<Capabilities> capabilities = getCapabilities(extensionContext,
                parameter, browserType, opBrowser);
        if (capabilities.isPresent()) {
            wdm.capabilities(capabilities.get());
        }
        return wdm;
    }

    private Optional<Capabilities> getCapabilities(
            ExtensionContext extensionContext, Parameter parameter,
            Optional<BrowserType> browserType, Optional<Browser> browser) {

        CapabilitiesHandler capsHandler = new CapabilitiesHandler(config,
                annotationsReader, parameter, extensionContext, browser,
                browserType, isGeneric(parameter.getType()));

        return capsHandler.getCapabilities();

    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext)
            throws Exception {
        String contextId = getContextId(extensionContext);
        ScreenshotManager screenshotManager = new ScreenshotManager(
                extensionContext, getConfig(), outputHandler);

        if (wdmMap.containsKey(contextId)) {
            wdmMap.get(contextId).stream()
                    .map(WebDriverManager::getWebDriverList)
                    .forEach(screenshotManager::makeScreenshotIfRequired);
            wdmMap.get(contextId).stream()
                    .forEach(WebDriverManager::stopDockerRecording);
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        if (!isSingleSession(extensionContext)) {
            quitWebDriver(extensionContext);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (isSingleSession(extensionContext)) {
            quitWebDriver(extensionContext);
        }
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        boolean allWebDriver = false;
        if (context.getTestMethod().isPresent()) {
            allWebDriver = !stream(
                    context.getTestMethod().get().getParameterTypes())
                            .map(s -> s.equals(WebDriver.class)
                                    || s.equals(RemoteWebDriver.class)
                                    || selenideHandler.isSelenide(s))
                            .collect(toList()).contains(false);
        }
        return allWebDriver;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext) {
        String contextId = getContextId(extensionContext);
        try {
            // Registered browsers
            if (!browserListList.isEmpty()) {
                return browserListList.stream()
                        .map(b -> invocationContext(b, this));
            }

            // Browser scenario by content
            String browserJsonContent = config.getBrowserTemplateJsonContent();
            if (browserJsonContent.isEmpty()) {

                // Browser scenario by JSON file
                String browserJsonFile = config.getBrowserTemplateJsonFile();
                if (browserJsonFile.startsWith(CLASSPATH_PREFIX)) {
                    String browserJsonInClasspath = browserJsonFile
                            .substring(CLASSPATH_PREFIX.length());
                    InputStream resourceAsStream = this.getClass()
                            .getResourceAsStream("/" + browserJsonInClasspath);
                    if (resourceAsStream != null) {
                        browserJsonContent = IOUtils.toString(resourceAsStream,
                                defaultCharset());
                    }

                } else {
                    browserJsonContent = new String(
                            readAllBytes(get(browserJsonFile)));
                }
            }

            if (!browserJsonContent.isEmpty()) {
                return new Gson()
                        .fromJson(browserJsonContent, BrowsersTemplate.class)
                        .getStream().map(b -> invocationContext(b, this));
            }

            if (browserListMap != null) {
                List<Browser> browsers = browserListMap.get(contextId);
                if (browsers != null) {
                    return Stream.of(invocationContext(browsers, this));
                } else {
                    return Stream.empty();
                }
            }

        } catch (IOException e) {
            throw new SeleniumJupiterException(e);
        }

        throw new SeleniumJupiterException(
                "No browser scenario registered for test template");
    }

    private synchronized TestTemplateInvocationContext invocationContext(
            List<Browser> template, SeleniumJupiter parent) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return template.toString();
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return singletonList(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(
                            ParameterContext parameterContext,
                            ExtensionContext extensionContext) {
                        Class<?> type = parameterContext.getParameter()
                                .getType();
                        return type.equals(WebDriver.class)
                                || type.equals(RemoteWebDriver.class)
                                || selenideHandler.isSelenide(type);
                    }

                    @Override
                    public Object resolveParameter(
                            ParameterContext parameterContext,
                            ExtensionContext extensionContext) {
                        String contextId = getContextId(extensionContext);
                        log.trace("Setting browser list {} for context id {}",
                                template, contextId);
                        parent.browserListMap.put(contextId, template);

                        return parent.resolveParameter(parameterContext,
                                extensionContext);
                    }
                });
            }
        };
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(
            ExtensionContext context) {
        AnnotatedElement element = context.getElement().orElse(null);

        return findAnnotation(element, EnabledIfBrowserAvailable.class)
                .map(this::toBrowserResult)
                .orElse(findAnnotation(element, EnabledIfDriverUrlOnline.class)
                        .map(this::toUrlResult)
                        .orElse(findAnnotation(element,
                                EnabledIfDockerAvailable.class)
                                        .map(this::toDockerResult)
                                        .orElse(ENABLED)));
    }

    public Config getConfig() {
        return config;
    }

    public void addBrowsers(Browser... browsers) {
        browserListList.add(asList(browsers));
    }

    public void addBrowsers(String[]... browsers) {
        asList(browsers).stream().forEach(this::addBrowsers);
    }

    public void addBrowsers(String... browsers) {
        browserListList.add(asList(browsers).stream()
                .map(browser -> new BrowserBuilder(browser).build())
                .collect(Collectors.toList()));
    }

    public void putBrowserList(String key, List<Browser> browserList) {
        browserListMap.put(key, browserList);
    }

    private boolean isTestTemplate(ExtensionContext extensionContext) {
        Optional<Method> testMethod = extensionContext.getTestMethod();
        return testMethod.isPresent()
                && testMethod.get().isAnnotationPresent(TestTemplate.class);
    }

    private boolean isGeneric(Class<?> type) {
        return isGeneric(type.getCanonicalName());
    }

    private boolean isGeneric(String type) {
        return type.equals("org.openqa.selenium.remote.RemoteWebDriver")
                || type.equals("org.openqa.selenium.WebDriver");
    }

    private Browser getBrowser(String contextId, int index) {
        log.trace("Getting browser by contextId {} and index {}", contextId,
                index);
        Browser browser = null;

        if (!browserListMap.containsKey(contextId)) {
            log.warn("Browser list for context id {} not found", contextId);
        } else {
            List<Browser> browserList = browserListMap.get(contextId);

            if (index >= browserList.size()) {
                index = browserList.size() - 1;
            }
            browser = browserList.get(index);
        }
        return browser;
    }

    private ConditionEvaluationResult toDockerResult(
            EnabledIfDockerAvailable annotation) {
        if (!WebDriverManager.isDockerAvailable()) {
            return ConditionEvaluationResult
                    .disabled("Docker is not installed in the system");
        }
        return ENABLED;
    }

    private ConditionEvaluationResult toBrowserResult(
            EnabledIfBrowserAvailable annotation) {
        io.github.bonigarcia.seljup.Browser[] browsers = annotation.value();
        for (io.github.bonigarcia.seljup.Browser browser : browsers) {
            DriverManagerType driverManagerType = DriverManagerType
                    .valueOf(browser.name());
            Optional<Path> browserPath = WebDriverManager
                    .getInstance(driverManagerType).getBrowserPath();

            if (!browserPath.isPresent()) {
                return ConditionEvaluationResult
                        .disabled(browser + " is not installed in the system");
            }
        }
        return ENABLED;
    }

    private ConditionEvaluationResult toUrlResult(
            EnabledIfDriverUrlOnline annotation) {
        String urlValue = annotation.value();
        ConditionEvaluationResult disabled = ConditionEvaluationResult
                .disabled(urlValue + " is not online");
        try {
            URL url = new URL(urlValue);
            if (!isOnline(url)) {
                URL urlStatus = new URL(url, "/status");
                if (!isOnline(urlStatus)) {
                    return disabled;
                }
            }
            urlFromAnnotation = url;
        } catch (MalformedURLException e) {
            return disabled;
        }
        return ENABLED;
    }

    private void removeManagersFromMap(String contextId) {
        if (wdmMap.containsKey(contextId)) {
            wdmMap.remove(contextId);
            log.trace("Removing managers from map (id {})", contextId);
        }
    }

    private void putManagerInMap(String contextId, WebDriverManager wdm) {
        log.trace("Put manager {} in map (context id {})", wdm, contextId);
        if (wdmMap.containsKey(contextId)) {
            wdmMap.get(contextId).add(wdm);
            log.trace("Adding {} to existing map (id {})", wdm, contextId);
        } else {
            List<WebDriverManager> wdmList = new ArrayList<>();
            wdmList.add(wdm);
            wdmMap.put(contextId, wdmList);
            log.trace("Adding {} to new map (id {})", wdm, contextId);
        }
    }

    private boolean isSingleSession(ExtensionContext extensionContext) {
        boolean singleSession = false;
        Optional<Class<?>> testClass = extensionContext.getTestClass();
        if (testClass.isPresent()) {
            singleSession = testClass.get()
                    .isAnnotationPresent(SingleSession.class);
        }
        log.trace("Single session {}", singleSession);
        return singleSession;
    }

    private void quitWebDriver(ExtensionContext extensionContext) {
        String contextId = getContextId(extensionContext);

        log.trace("Quitting contextId {}: (wdmMap={})", contextId, wdmMap);

        // Close DevTools (if any)
        devToolsList.forEach(DevTools::close);
        devToolsList.clear();

        if (wdmMap.containsKey(contextId)) {
            Optional<Throwable> executionException = extensionContext
                    .getExecutionException();
            wdmMap.get(contextId).forEach(manager -> {

                // Get recording files (to be deleted after quit)
                List<Path> recordingList = Collections.emptyList();
                if (config.isRecordingWhenFailure()
                        && !executionException.isPresent()) {
                    recordingList = manager.getWebDriverList().stream()
                            .map(manager::getDockerRecordingPath)
                            .collect(Collectors.toList());
                }

                // Quit manager
                manager.quit();

                // Delete recordings (if any)
                recordingList.forEach(path -> {
                    try {
                        log.debug("Deleting {} (since test does not fail)",
                                path);
                        Files.delete(path);
                    } catch (Exception e) {
                        log.warn("Exception trying to delete recording {}",
                                path);
                    }
                });
            });

            removeManagersFromMap(contextId);
        }
    }

    public URL getDockerNoVncUrl() {
        URL url = null;
        if (!wdmMap.isEmpty()) {
            List<WebDriverManager> wdmList = wdmMap.entrySet().iterator().next()
                    .getValue();
            return wdmList.get(0).getDockerNoVncUrl();
        }
        return url;
    }

    public URL getDockerNoVncUrl(WebDriver driver) {
        URL dockerNoVncUrl = null;
        if (!wdmMap.isEmpty()) {
            for (List<WebDriverManager> wdmList : wdmMap.values()) {
                for (WebDriverManager wdm : wdmList) {
                    dockerNoVncUrl = wdm.getDockerNoVncUrl(driver);
                    if (dockerNoVncUrl != null) {
                        return dockerNoVncUrl;
                    }
                }
            }
        }
        return dockerNoVncUrl;
    }

}
