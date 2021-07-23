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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
import io.github.bonigarcia.seljup.handler.DriverHandler;
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

    // TODO refactor this logic
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
        BrowserType browserType = null;
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
        if (isSingleSession(extensionContext)
                && wdmMap.containsKey(contextId)) {
            List<WebDriverManager> list = wdmMap.get(contextId);
            if (index < list.size()) {
                Object obj = list.get(index).getWebDriver();
                if (obj != null) {
                    log.trace("Returning index {}: {}", index, obj);
                    return obj;
                }
            }
        }

        if (isGeneric && !browserListMap.isEmpty()) {
            // Template
            browser = getBrowser(contextId, index);
            if (browser != null) {
                browserType = browser.toBrowserType();
                wdm = WebDriverManager.getInstance(browserType.toBrowserName())
                        .browserVersion(browser.getVersion())
                        .remoteAddress(browser.getUrl());
                if (browser.isDockerBrowser()) {
                    wdm.browserInDocker();
                }
                if (browser.isAndroidBrowser()) {
                    wdm.browserInDockerAndroid();
                }
            } else {
                wdm = WebDriverManager.getInstance();
            }

            Optional<Capabilities> capabilities = getCapabilities(
                    extensionContext, parameter, browserType);
            if (capabilities.isPresent()) {
                wdm.capabilities(capabilities.get());
            }

        } else if (dockerBrowser.isPresent()) {
            // Docker
            browserType = dockerBrowser.get().type();
            String browserVersion = dockerBrowser.get().version();
            wdm = WebDriverManager.getInstance(browserType.toBrowserName())
                    .browserVersion(browserVersion).browserInDocker();
            if (browserType == CHROME_MOBILE) {
                wdm.browserInDockerAndroid();
            }
            if (dockerBrowser.get().recording()) {
                wdm.enableRecording();
            }
            if (dockerBrowser.get().vnc()) {
                wdm.enableVnc();
            }
            if (dockerBrowser.get().size() > 1) {
                browserNumber = dockerBrowser.get().size();
            }
            Optional<Capabilities> capabilities = getCapabilities(
                    extensionContext, parameter, browserType);
            if (capabilities.isPresent()) {
                wdm.capabilities(capabilities.get());
            }

        } else if (url.isPresent() && caps.isPresent()) {
            // Remote
            wdm = WebDriverManager.getInstance()
                    .remoteAddress(url.get().toString())
                    .capabilities(caps.get());

        } else {
            // Local
            if (type == List.class) {
                throw new SeleniumJupiterException(
                        "List<WebDriver> must be used together with @DockerBrowser");
            }
            if (isGeneric) {
                wdm = WebDriverManager.getInstance();
            } else {
                wdm = WebDriverManager.getInstance(type);
            }

            Optional<Capabilities> capabilities = getCapabilities(
                    extensionContext, parameter, browserType);
            if (capabilities.isPresent()) {
                wdm.capabilities(capabilities.get());
            }
        }

        putManagerInMap(contextId, wdm);

        return browserNumber == 1 ? wdm.create() : wdm.create(browserNumber);
    }

    private Optional<Capabilities> getCapabilities(
            ExtensionContext extensionContext, Parameter parameter,
            BrowserType browserType) {
        Optional<DriverHandler> driverHandler = DriverHandler.getInstance(
                browserType, parameter, extensionContext, config,
                annotationsReader);
        if (driverHandler.isPresent()) {
            return Optional.of(driverHandler.get().getCapabilities());
        }
        return Optional.empty();
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext)
            throws Exception {
        // 1. Screenshots (if required)
        String contextId = extensionContext.getUniqueId();
        ScreenshotManager screenshotManager = new ScreenshotManager(
                extensionContext, getConfig());
        getValueFromMapUsingContextId(wdmMap, contextId).stream()
                .map(WebDriverManager::getWebDriverList)
                .forEach(driverList -> screenshotManager
                        .makeScreenshotIfRequired(screenshotManager,
                                extensionContext, driverList));

        // 2. Quit WebDriver
        if (!isSingleSession(extensionContext)) {
            quitWebDriver(contextId);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        String contextId = extensionContext.getUniqueId();
        if (isSingleSession(extensionContext)) {
            quitWebDriver(contextId);
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
        Browser browser = null;
        List<Browser> browserList = getValueFromMapUsingContextId(
                browserListMap, contextId);
        if (browserList == null) {
            log.warn("Browser list for context id {} not found", contextId);
        } else {
            if (index >= browserList.size()) {
                index = browserList.size() - 1;
            }
            browser = browserList.get(index);
        }
        return browser;
    }

    private <T extends Object> T getValueFromMapUsingContextId(
            Map<String, T> map, String contextId) {
        String newContextId = searchContextIdKeyInMap(map, contextId);
        return map.get(newContextId);
    }

    private String searchContextIdKeyInMap(Map<String, ?> map,
            String contextId) {
        if (!map.containsKey(contextId)) {
            int i = contextId.lastIndexOf('/');
            if (i != -1) {
                contextId = contextId.substring(0, i);
            }
        }
        return contextId;
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
        String newContextId = searchContextIdKeyInMap(wdmMap, contextId);
        log.trace("Put manager {} in map (context id {}, new context id {})",
                wdm, contextId, newContextId);
        if (wdmMap.containsKey(contextId)) {
            wdmMap.get(contextId).add(wdm);
            log.trace("Adding {} to existing map (id {})", wdm, contextId);
        } else if (wdmMap.containsKey(newContextId)) {
            wdmMap.get(newContextId).add(wdm);
            log.trace("Adding {} to existing map (new id {})", wdm,
                    newContextId);
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

    private void quitWebDriver(String contextId) {
        getValueFromMapUsingContextId(wdmMap, contextId)
                .forEach(WebDriverManager::quit);
        removeManagersFromMap(contextId);
    }

}
