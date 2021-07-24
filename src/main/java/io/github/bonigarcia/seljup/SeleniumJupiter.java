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

import static io.github.bonigarcia.seljup.BrowserType.CHROME_MOBILE;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.AfterAllCallback;
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
        AfterTestExecutionCallback, AfterAllCallback,
        TestTemplateInvocationContextProvider, ExecutionCondition {

    final Logger log = getLogger(lookup().lookupClass());

    static final String CLASSPATH_PREFIX = "classpath:";
    static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
            .enabled("Browser(s) available in the system");

    Config config;
    Map<String, List<WebDriverManager>> wdmMap;
    AnnotationsReader annotationsReader;
    List<List<Browser>> browserListList;
    Map<String, List<Browser>> browserListMap;

    public SeleniumJupiter() {
        config = new Config();
        wdmMap = new ConcurrentHashMap<>();
        annotationsReader = new AnnotationsReader();
        browserListList = new ArrayList<>();
        browserListMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return (WebDriver.class.isAssignableFrom(type)
                || type.equals(List.class))
                && !isTestTemplate(extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        String contextId = extensionContext.getUniqueId();
        Parameter parameter = parameterContext.getParameter();
        int index = parameterContext.getIndex();
        Optional<Object> testInstance = extensionContext.getTestInstance();

        log.trace("Resolving parameter {} (contextId {}, index {})", parameter,
                contextId, index);

        WebDriverManager wdm = null;
        Browser browser = null;
        int browserNumber = 1;

        Class<?> type = parameter.getType();
        boolean isGeneric = isGeneric(type);
        Optional<DockerBrowser> dockerBrowser = annotationsReader
                .getDocker(parameter);
        Optional<URL> url = annotationsReader.getUrl(parameter, testInstance,
                config.getSeleniumServerUrl());
        Optional<Capabilities> caps = annotationsReader
                .getCapabilities(parameter, testInstance);

        // Single session
        Optional<List<WebDriverManager>> wdmList = getValueFromMapStartingWithKey(
                wdmMap, contextId);
        if (isSingleSession(extensionContext) && wdmList.isPresent()
                && index < wdmList.get().size()) {
            WebDriver driver = wdmList.get().get(index).getWebDriver();
            if (driver != null) {
                log.trace("Returning driver at index {}: {}", index, driver);
                return driver;
            }
        }

        if (config.getManager() != null) { // Custom manager
            wdm = config.getManager();

        } else if (isGeneric && !browserListMap.isEmpty()) { // Template
            browser = getBrowser(contextId, index);
            wdm = getManagerForTemplate(extensionContext, parameter, browser,
                    url);

        } else if (dockerBrowser.isPresent()) { // Docker
            if (dockerBrowser.get().size() > 1) {
                browserNumber = dockerBrowser.get().size();
            }
            wdm = getManagerForDocker(extensionContext, parameter,
                    dockerBrowser.get());

        } else if (url.isPresent() && caps.isPresent()) { // Remote
            wdm = getManagerForRemote(url.get(), caps.get());

        } else { // Local
            wdm = getManagerForLocal(extensionContext, parameter, type,
                    isGeneric);
        }

        putManagerInMap(contextId, wdm);

        return browserNumber == 1 ? wdm.create() : wdm.create(browserNumber);
    }

    private WebDriverManager getManagerForRemote(URL url, Capabilities caps) {
        WebDriverManager wdm;
        wdm = WebDriverManager.getInstance().remoteAddress(url.toString())
                .capabilities(caps);
        return wdm;
    }

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
            wdm = WebDriverManager.getInstance(type);
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
            OutputHandler outputHandler = new OutputHandler(extensionContext,
                    getConfig());
            wdm.recordingPrefix(outputHandler.getPrefix());
        }
        if (dockerBrowser.vnc() || config.isVnc()) {
            wdm.enableVnc();
        }
        if (dockerBrowser.volumes().length > 0) {
            wdm.dockerVolumes(dockerBrowser.volumes());
        }
        if (!dockerBrowser.lang().isBlank()) {
            wdm.dockerLang(dockerBrowser.lang());
        }
        if (!dockerBrowser.timezone().isBlank()) {
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
                browserType);

        return capsHandler.getCapabilities();

    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext)
            throws Exception {
        // 1. Screenshots (if required)
        String contextId = extensionContext.getUniqueId();
        OutputHandler outputHandler = new OutputHandler(extensionContext,
                getConfig());
        ScreenshotManager screenshotManager = new ScreenshotManager(
                extensionContext, getConfig(), outputHandler);
        Optional<List<WebDriverManager>> mapUsingContextId = getValueFromMapStartingWithKey(
                wdmMap, contextId);
        if (mapUsingContextId.isPresent()) {
            mapUsingContextId.get().stream()
                    .map(WebDriverManager::getWebDriverList)
                    .forEach(screenshotManager::makeScreenshotIfRequired);
        }

        // 2. Quit WebDriver
        if (!isSingleSession(extensionContext)) {
            quitWebDriver(contextId, extensionContext);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        String contextId = extensionContext.getUniqueId();
        if (isSingleSession(extensionContext)) {
            quitWebDriver(contextId, extensionContext);
        }
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        boolean allWebDriver = false;
        if (context.getTestMethod().isPresent()) {
            allWebDriver = !stream(
                    context.getTestMethod().get().getParameterTypes())
                            .map(s -> s.equals(WebDriver.class)
                                    || s.equals(RemoteWebDriver.class))
                            .collect(toList()).contains(false);
        }
        return allWebDriver;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext) {
        String contextId = extensionContext.getUniqueId();
        try {
            // 1. By JSON content
            String browserJsonContent = config.getBrowserTemplateJsonContent();
            if (browserJsonContent.isEmpty()) {
                // 2. By JSON file
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

            // 3. By setter
            if (!browserListList.isEmpty()) {
                return browserListList.stream()
                        .map(b -> invocationContext(b, this));
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
                                || type.equals(RemoteWebDriver.class);
                    }

                    @Override
                    public Object resolveParameter(
                            ParameterContext parameterContext,
                            ExtensionContext extensionContext) {
                        String contextId = extensionContext.getUniqueId();
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
                .map(this::toResult).orElse(ENABLED);
    }

    public Config getConfig() {
        return config;
    }

    public void addBrowsers(Browser... browsers) {
        browserListList.add(asList(browsers));
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
        return type.equals(RemoteWebDriver.class)
                || type.equals(WebDriver.class);
    }

    private Browser getBrowser(String contextId, int index) {
        log.trace("Getting browser by contextId {} and index {}", contextId,
                index);
        Browser browser = null;
        Optional<List<Browser>> browserList = getValueFromMapStartingWithKey(
                browserListMap, contextId);
        if (browserList.isEmpty()) {
            log.warn("Browser list for context id {} not found", contextId);
        } else {
            if (index >= browserList.get().size()) {
                index = browserList.get().size() - 1;
            }
            browser = browserList.get().get(index);
        }
        return browser;
    }

    private <T extends Object> Optional<T> getValueFromMapStartingWithKey(
            Map<String, T> map, String searchKey) {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (entry.getKey().startsWith(searchKey)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private ConditionEvaluationResult toResult(
            EnabledIfBrowserAvailable annotation) {
        io.github.bonigarcia.seljup.Browser[] browsers = annotation.value();
        for (io.github.bonigarcia.seljup.Browser browser : browsers) {
            DriverManagerType driverManagerType = DriverManagerType
                    .valueOf(browser.name());
            Optional<Path> browserPath = WebDriverManager
                    .getInstance(driverManagerType).getBrowserPath();

            if (browserPath.isEmpty()) {
                return ConditionEvaluationResult
                        .disabled(browser + " is not installed in the system");
            }
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
        Optional<List<WebDriverManager>> mapUsingContextId = getValueFromMapStartingWithKey(
                wdmMap, contextId);
        if (mapUsingContextId.isPresent()) {
            mapUsingContextId.get().add(wdm);
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

    private void quitWebDriver(String contextId,
            ExtensionContext extensionContext) {
        Optional<List<WebDriverManager>> mapByContextId = getValueFromMapStartingWithKey(
                wdmMap, contextId);
        log.trace("Map by contextId {}: {} (wdmMap={})", contextId,
                mapByContextId, wdmMap);
        if (mapByContextId.isPresent()) {
            Optional<Throwable> executionException = extensionContext
                    .getExecutionException();
            mapByContextId.get().forEach(manager -> {
                // Get recording files (to be deleted after quit)
                List<Path> recordingList = Collections.emptyList();
                if (config.isRecordingWhenFailure()
                        && executionException.isEmpty()) {
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

}
