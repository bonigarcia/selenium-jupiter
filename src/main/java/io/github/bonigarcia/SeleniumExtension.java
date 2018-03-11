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

import static io.github.bonigarcia.SeleniumJupiter.getBoolean;
import static io.github.bonigarcia.SeleniumJupiter.getString;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private List<Class<?>> typeList = new ArrayList<>();
    private List<DriverHandler> driverHandlerList = new ArrayList<>();
    private Map<String, Class<?>> handlerMap = new HashMap<>();
    private Map<String, Class<?>> templateHandlerMap = new HashMap<>();
    private Map<String, DockerContainer> containerMap = new LinkedHashMap<>();
    private List<Browser> browserList;
    private DockerService dockerService;
    private SelenoidConfig selenoidConfig;

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
        addEntry(templateHandlerMap, "chrome-in-docker", RemoteWebDriver.class);
        addEntry(templateHandlerMap, "firefox-in-docker",
                RemoteWebDriver.class);
        addEntry(templateHandlerMap, "opera-in-docker", RemoteWebDriver.class);
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

        Parameter parameter = parameterContext.getParameter();
        Class<?> type = parameter.getType();
        boolean isTemplate = isTestTemplate(extensionContext);
        boolean isGeneric = type.equals(RemoteWebDriver.class)
                || type.equals(WebDriver.class);

        // Check template
        Integer index = null;
        if (isGeneric && browserList != null) {
            index = isTemplate
                    ? Integer.valueOf(parameter.getName().replaceAll("arg", ""))
                    : 0;
            type = templateHandlerMap.get(browserList.get(index).getType());
        }

        // WebDriverManager
        if (!typeList.contains(type)) {
            typeList.add(type);
            WebDriverManager.getInstance(type).setup();
        }

        // Handler
        DriverHandler driverHandler = null;
        Class<?> constructorClass = handlerMap.containsKey(type.getName())
                ? handlerMap.get(type.getName())
                : OtherDriverHandler.class;
        boolean isRemote = constructorClass.equals(RemoteDriverHandler.class);

        try {
            driverHandler = getDriverHandler(extensionContext, parameter, type,
                    index, constructorClass, isRemote);

            if (type.equals(RemoteWebDriver.class)
                    || type.equals(WebDriver.class)
                    || type.equals(List.class)) {
                initHandlerForDocker(driverHandler);
            }

            if (!isTemplate && isGeneric && isRemote) {
                ((RemoteDriverHandler) driverHandler).setParent(this);
                ((RemoteDriverHandler) driverHandler)
                        .setParameterContext(parameterContext);
            }

            driverHandlerList.add(driverHandler);
        } catch (Exception e) {
            handleException(parameter, driverHandler, constructorClass, e);
        }

        if (driverHandler != null) {
            driverHandler.resolve();
            return driverHandler.getObject();
        } else if (getBoolean("sel.jup.exception.when.no.driver")) {
            throw new SeleniumJupiterException(
                    "No valid handler for " + parameter + " was found");
        } else {
            return null;
        }
    }

    private DriverHandler getDriverHandler(ExtensionContext extensionContext,
            Parameter parameter, Class<?> type, Integer index,
            Class<?> constructorClass, boolean isRemote)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        DriverHandler driverHandler;
        if (isRemote && browserList != null) {
            driverHandler = (DriverHandler) constructorClass
                    .getDeclaredConstructor(Parameter.class,
                            ExtensionContext.class, Browser.class)
                    .newInstance(parameter, extensionContext,
                            browserList.get(index));

        } else if (constructorClass.equals(OtherDriverHandler.class)
                && browserList != null) {
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

    private void initHandlerForDocker(DriverHandler driverHandler)
            throws DockerCertificateException {
        if (containerMap == null) {
            containerMap = new LinkedHashMap<>();
        }
        driverHandler.setContainerMap(containerMap);

        if (dockerService == null) {
            dockerService = new DockerService();
        }
        driverHandler.setDockerService(dockerService);

        if (selenoidConfig == null) {
            selenoidConfig = new SelenoidConfig();
        }
        driverHandler.setSelenoidConfig(selenoidConfig);
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
    public void afterEach(ExtensionContext context) {
        // Make screenshots if required and close browsers
        ScreenshotManager screenshotManager = new ScreenshotManager(context);
        for (DriverHandler driverHandler : driverHandlerList) {
            try {
                Object object = driverHandler.getObject();
                if (object == null) {
                    continue;
                }
                if (List.class.isAssignableFrom(object.getClass())) {
                    List<RemoteWebDriver> webDriverList = (List<RemoteWebDriver>) object;
                    for (int i = 0; i < webDriverList.size(); i++) {
                        screenshotManager.makeScreenshot(webDriverList.get(i),
                                driverHandler.getName() + "_" + i);
                        webDriverList.get(i).quit();
                    }

                } else {
                    WebDriver webDriver = (WebDriver) object;
                    screenshotManager.makeScreenshot(webDriver,
                            driverHandler.getName());
                    webDriver.quit();
                }
            } catch (Exception e) {
                log.warn("Exception closing webdriver instance", e);
            }
        }

        // Clean handlers
        for (DriverHandler driverHandler : driverHandlerList) {
            try {
                driverHandler.cleanup();
            } catch (Exception e) {
                log.warn("Exception cleaning handler {}", driverHandler, e);
            }
        }

        // Clear handler list
        driverHandlerList.clear();
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
        BrowsersTemplate browsersTemplate = null;
        try {
            String browserJsonContent = getString(
                    "sel.jup.browser.template.json.content");

            if (browserJsonContent.isEmpty()) {
                String browserJsonFile = getString(
                        "sel.jup.browser.template.json.file");
                if (browserJsonFile.startsWith(CLASSPATH_PREFIX)) {
                    String browserJsonInClasspath = browserJsonFile
                            .substring(CLASSPATH_PREFIX.length());
                    browserJsonContent = IOUtils.toString(
                            this.getClass().getResourceAsStream(
                                    "/" + browserJsonInClasspath),
                            defaultCharset());
                } else {
                    browserJsonContent = new String(
                            readAllBytes(get(browserJsonFile)));
                }
            }

            browsersTemplate = new Gson().fromJson(browserJsonContent,
                    BrowsersTemplate.class);

        } catch (IOException e) {
            throw new SeleniumJupiterException(e);
        }
        return browsersTemplate.getStream()
                .map(b -> invocationContext(b, this));
    }

    private TestTemplateInvocationContext invocationContext(
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
                        parent.browserList = template;
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

    public void setBrowserList(List<Browser> browserList) {
        this.browserList = browserList;
    }

}
