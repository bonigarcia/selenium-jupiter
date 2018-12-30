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
package io.github.bonigarcia;

import static io.github.bonigarcia.SeleniumJupiter.config;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.spotify.docker.client.exceptions.DockerCertificateException;

import io.appium.java_client.AppiumDriver;
import io.github.bonigarcia.BrowsersTemplate.Browser;
import io.github.bonigarcia.handler.AppiumDriverHandler;
import io.github.bonigarcia.handler.ChromeDriverHandler;
import io.github.bonigarcia.handler.DriverHandler;
import io.github.bonigarcia.handler.EdgeDriverHandler;
import io.github.bonigarcia.handler.FirefoxDriverHandler;
import io.github.bonigarcia.handler.ListDriverHandler;
import io.github.bonigarcia.handler.OperaDriverHandler;
import io.github.bonigarcia.handler.OtherDriverHandler;
import io.github.bonigarcia.handler.RemoteDriverHandler;
import io.github.bonigarcia.handler.SafariDriverHandler;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Selenium extension for Jupiter (JUnit 5) tests.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumExtension implements ParameterResolver, AfterEachCallback,
        TestTemplateInvocationContextProvider {

    final Logger log = getLogger(lookup().lookupClass());

    static final String CLASSPATH_PREFIX = "classpath:";

    private List<Class<?>> typeList = new CopyOnWriteArrayList<>();
    private Map<String, DriverHandler> driverHandlerMap = new ConcurrentHashMap<>();
    private Map<String, Class<?>> handlerMap = new ConcurrentHashMap<>();
    private Map<String, Class<?>> templateHandlerMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, DockerContainer>> containersMap = new ConcurrentHashMap<>();
    private DockerService dockerService;
    private Map<String, List<Browser>> browserListMap = new ConcurrentHashMap<>();
    private List<List<Browser>> browserListList;

    public SeleniumExtension() {
        addEntry(handlerMap, "org.openqa.selenium.chrome.ChromeDriver",
                ChromeDriverHandler.class);
        addEntry(handlerMap, "org.openqa.selenium.firefox.FirefoxDriver",
                FirefoxDriverHandler.class);
        addEntry(handlerMap, "org.openqa.selenium.edge.EdgeDriver",
                EdgeDriverHandler.class);
        addEntry(handlerMap, "org.openqa.selenium.opera.OperaDriver",
                OperaDriverHandler.class);
        addEntry(handlerMap, "org.openqa.selenium.safari.SafariDriver",
                SafariDriverHandler.class);
        addEntry(handlerMap, "org.openqa.selenium.remote.RemoteWebDriver",
                RemoteDriverHandler.class);
        addEntry(handlerMap, "org.openqa.selenium.WebDriver",
                RemoteDriverHandler.class);
        addEntry(handlerMap, "io.appium.java_client.AppiumDriver",
                AppiumDriverHandler.class);
        addEntry(handlerMap, "java.util.List", ListDriverHandler.class);
        addEntry(handlerMap, "org.openqa.selenium.phantomjs.PhantomJSDriver",
                OtherDriverHandler.class);

        addEntry(templateHandlerMap, "chrome", ChromeDriver.class);
        addEntry(templateHandlerMap, "firefox", FirefoxDriver.class);
        addEntry(templateHandlerMap, "edge", EdgeDriver.class);
        addEntry(templateHandlerMap, "opera", OperaDriver.class);
        addEntry(templateHandlerMap, "safari", SafariDriver.class);
        addEntry(templateHandlerMap, "appium", AppiumDriver.class);
        addEntry(templateHandlerMap, "phantomjs", PhantomJSDriver.class);
        addEntry(templateHandlerMap, "iexplorer", InternetExplorerDriver.class);
        addEntry(templateHandlerMap, "chrome-in-docker", RemoteWebDriver.class);
        addEntry(templateHandlerMap, "firefox-in-docker",
                RemoteWebDriver.class);
        addEntry(templateHandlerMap, "opera-in-docker", RemoteWebDriver.class);
        addEntry(templateHandlerMap, "android", RemoteWebDriver.class);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return (WebDriver.class.isAssignableFrom(type)
                || type.equals(List.class))
                && !isTestTemplate(extensionContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object resolveParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {

        String contextId = extensionContext.getUniqueId();
        Parameter parameter = parameterContext.getParameter();
        Class<?> type = parameter.getType();
        boolean isTemplate = isTestTemplate(extensionContext);
        boolean isGeneric = type.equals(RemoteWebDriver.class)
                || type.equals(WebDriver.class);
        String url = null;

        // Check template
        Integer index = null;
        if (isGeneric && !browserListMap.isEmpty()) {
            index = isTemplate
                    ? Integer.valueOf(parameter.getName().replaceAll("arg", ""))
                    : 0;
            List<Browser> browserListFromContextId = (List<Browser>) getValueFromContextId(
                    browserListMap, contextId);
            if (browserListFromContextId == null) {
                log.warn("Browser list for context id {} not found", contextId);
            } else {
                type = templateHandlerMap
                        .get(browserListFromContextId.get(index).getType());
                url = browserListFromContextId.get(index).getUrl();
            }
        }

        // WebDriverManager
        if (!typeList.contains(type)) {
            WebDriverManager.getInstance(type).setup();
            typeList.add(type);
        }

        // Handler
        DriverHandler driverHandler = null;
        Class<?> constructorClass = handlerMap.containsKey(type.getName())
                ? handlerMap.get(type.getName())
                : OtherDriverHandler.class;
        boolean isRemote = constructorClass.equals(RemoteDriverHandler.class);

        if (url != null && !url.isEmpty()) {
            constructorClass = RemoteDriverHandler.class;
            isRemote = true;
        }

        try {
            driverHandler = getDriverHandler(extensionContext, parameter, type,
                    index, constructorClass, isRemote);

            if (type.equals(RemoteWebDriver.class)
                    || type.equals(WebDriver.class)
                    || type.equals(List.class)) {
                initHandlerForDocker(contextId, driverHandler);
            }

            if (!isTemplate && isGeneric && isRemote) {
                ((RemoteDriverHandler) driverHandler).setParent(this);
                ((RemoteDriverHandler) driverHandler)
                        .setParameterContext(parameterContext);
            }

            driverHandlerMap.put(extensionContext.getUniqueId(), driverHandler);
            log.trace("Adding {} to handler map (id {})", driverHandler,
                    extensionContext.getUniqueId());
        } catch (Exception e) {
            handleException(parameter, driverHandler, constructorClass, e);
        }

        if (driverHandler != null) {
            driverHandler.resolve();
            return driverHandler.getObject();
        } else if (config().isExceptionWhenNoDriver()) {
            throw new SeleniumJupiterException(
                    "No valid handler for " + parameter + " was found");
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private DriverHandler getDriverHandler(ExtensionContext extensionContext,
            Parameter parameter, Class<?> type, Integer index,
            Class<?> constructorClass, boolean isRemote)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        DriverHandler driverHandler = null;
        String contextId = extensionContext.getUniqueId();
        if (isRemote && !browserListMap.isEmpty()) {
            List<Browser> browserListFromContextId = (List<Browser>) getValueFromContextId(
                    browserListMap, contextId);
            if (browserListFromContextId == null) {
                log.warn("Browser list for context id {} not found", contextId);
            } else {
                driverHandler = (DriverHandler) constructorClass
                        .getDeclaredConstructor(Parameter.class,
                                ExtensionContext.class, Browser.class)
                        .newInstance(parameter, extensionContext,
                                browserListFromContextId.get(index));
            }

        } else if (constructorClass.equals(OtherDriverHandler.class)
                && !browserListMap.isEmpty()) {
            driverHandler = (DriverHandler) constructorClass
                    .getDeclaredConstructor(Parameter.class,
                            ExtensionContext.class, Class.class)
                    .newInstance(parameter, extensionContext, type);

        } else {
            driverHandler = (DriverHandler) constructorClass
                    .getDeclaredConstructor(Parameter.class,
                            ExtensionContext.class)
                    .newInstance(parameter, extensionContext);

        }
        return driverHandler;
    }

    public void initHandlerForDocker(String contextId,
            DriverHandler driverHandler) throws DockerCertificateException {

        LinkedHashMap<String, DockerContainer> containerMap = new LinkedHashMap<>();
        driverHandler.setContainerMap(containerMap);
        containersMap.put(contextId, containerMap);

        if (dockerService == null) {
            dockerService = new DockerService();
        }
        driverHandler.setDockerService(dockerService);
    }

    private void handleException(Parameter parameter,
            DriverHandler driverHandler, Class<?> constructorClass,
            Exception e) {
        if (driverHandler != null
                && driverHandler.throwExceptionWhenNoDriver()) {
            log.error("Exception resolving {}", parameter, e);
            throw new SeleniumJupiterException(e);
        } else {
            log.warn("Exception creating {}", constructorClass, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterEach(ExtensionContext extensionContext) {
        // Make screenshots if required and close browsers
        ScreenshotManager screenshotManager = new ScreenshotManager(
                extensionContext);

        String contextId = extensionContext.getUniqueId();
        DriverHandler driverHandler = (DriverHandler) getValueFromContextId(
                driverHandlerMap, contextId);
        log.trace("After each for {} (id {})", driverHandler, contextId);
        if (driverHandler == null) {
            log.warn("Driver handler for context id {} not found", contextId);
            return;
        }

        try {
            Object object = driverHandler.getObject();
            if (object != null) {
                if (List.class.isAssignableFrom(object.getClass())) {
                    List<RemoteWebDriver> webDriverList = (List<RemoteWebDriver>) object;
                    for (int i = 0; i < webDriverList.size(); i++) {
                        screenshotManager.makeScreenshot(webDriverList.get(i),
                                driverHandler.getName() + "_" + i);
                        webDriverList.get(i).quit();
                    }

                } else {
                    WebDriver webDriver = (WebDriver) object;
                    if (driverHandler.getName() != null) {
                        screenshotManager.makeScreenshot(webDriver,
                                driverHandler.getName());
                    }
                    webDriver.quit();
                }
            }
        } catch (Exception e) {
            log.warn("Exception closing webdriver instance", e);
        }

        // Clean handler
        try {
            driverHandler.cleanup();
        } catch (Exception e) {
            log.warn("Exception cleaning handler {}", driverHandler, e);
        }

        // Clear handler map
        driverHandlerMap.remove(contextId);
    }

    private Object getValueFromContextId(Map<String, ?> map, String contextId) {
        Object output = map.get(contextId);
        if (output == null) {
            int i = contextId.lastIndexOf('/');
            if (i != -1) {
                contextId = contextId.substring(0, i);
                output = map.get(contextId);
            }
        }
        return output;
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
            String browserJsonContent = config()
                    .getBrowserTemplateJsonContent();
            if (browserJsonContent.isEmpty()) {
                // 2. By JSON file
                String browserJsonFile = config().getBrowserTemplateJsonFile();
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
            if (browserListList != null) {
                return browserListList.stream()
                        .map(b -> invocationContext(b, this));
            }
            if (browserListMap != null) {
                return Stream.of(
                        invocationContext(browserListMap.get(contextId), this));
            }

        } catch (IOException e) {
            throw new SeleniumJupiterException(e);
        }

        throw new SeleniumJupiterException(
                "No browser scenario registered for test template");
    }

    private synchronized TestTemplateInvocationContext invocationContext(
            List<Browser> template, SeleniumExtension parent) {
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

    private void addEntry(Map<String, Class<?>> map, String key,
            Class<?> value) {
        try {
            map.put(key, value);
        } catch (Exception e) {
            log.warn("Exception adding {}={} to handler map ({})", key, value,
                    e.getMessage());
        }
    }

    private boolean isTestTemplate(ExtensionContext extensionContext) {
        Optional<Method> testMethod = extensionContext.getTestMethod();
        return testMethod.isPresent()
                && testMethod.get().isAnnotationPresent(TestTemplate.class);
    }

    public void putBrowserList(String key, List<Browser> browserList) {
        this.browserListMap.put(key, browserList);
    }

    public void addBrowsers(Browser... browsers) {
        if (browserListList == null) {
            browserListList = new ArrayList<>();
        }
        browserListList.add(asList(browsers));
    }

    public String executeCommandInContainer(WebDriver driver,
            String... command) {
        try {
            for (String contextId : containersMap.keySet()) {
                DockerContainer selenoidContainer = containersMap.get(contextId)
                        .values().iterator().next();
                URL selenoidUrl = new URL(selenoidContainer.getContainerUrl());
                URL selenoidBaseUrl = new URL(selenoidUrl.getProtocol(),
                        selenoidUrl.getHost(), selenoidUrl.getPort(), "/");

                SelenoidService selenoidService = new SelenoidService(
                        selenoidBaseUrl.toString());
                Optional<String> containerId = selenoidService
                        .getContainerId(driver);

                if (containerId.isPresent()) {
                    return dockerService
                            .execCommandInContainer(containerId.get(), command);
                }
            }
            return "";

        } catch (Exception e) {
            throw new SeleniumJupiterException(e);
        }
    }

}
