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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.openqa.selenium.Platform.ANY;

import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.BrowserInstance;
import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumExtension;
import io.github.bonigarcia.seljup.SeleniumJupiterException;
import io.github.bonigarcia.seljup.WebDriverCreator;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Resolver for RemoteWebDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class RemoteDriverHandler extends DriverHandler {

    private DockerDriverHandler dockerDriverHandler;
    private Browser browser;
    private SeleniumExtension parent;
    private ParameterContext parameterContext;
    private WebDriverCreator webDriverCreator;

    public RemoteDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader) {
        super(parameter, context, config, annotationsReader);
    }

    public RemoteDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader,
            Browser browser) {
        super(parameter, context, config, annotationsReader);
        this.browser = browser;
    }

    @Override
    public void resolve() {
        try {
            Optional<Object> testInstance = context.getTestInstance();
            if (browser != null && browser.isDockerBrowser()) {
                BrowserInstance browserInstance = new BrowserInstance(config,
                        annotationsReader, browser.toBrowserType(),
                        browser.toCloudType(),
                        Optional.ofNullable(browser.getBrowserName()),
                        Optional.ofNullable(browser.getVolumes()));
                dockerDriverHandler = new DockerDriverHandler(context,
                        parameter, testInstance, annotationsReader,
                        containerMap, dockerService, config, browserInstance,
                        browser.getVersion());
                object = dockerDriverHandler.resolve(browserInstance,
                        browser.getVersion(), browser.getDeviceName(),
                        browser.getUrl(), true);
            } else {
                Optional<DockerBrowser> dockerBrowser = annotationsReader
                        .getDocker(parameter);
                if (dockerBrowser.isPresent()) {
                    BrowserInstance browserInstance = new BrowserInstance(
                            config, annotationsReader,
                            dockerBrowser.get().type(),
                            dockerBrowser.get().cloud(),
                            Optional.ofNullable(
                                    dockerBrowser.get().browserName()),
                            Optional.ofNullable(dockerBrowser.get().volumes()));
                    dockerDriverHandler = new DockerDriverHandler(context,
                            parameter, testInstance, annotationsReader,
                            containerMap, dockerService, config,
                            browserInstance, dockerBrowser.get().version());
                    object = dockerDriverHandler.resolve(dockerBrowser.get());
                } else {
                    resolveOtherThanDocker(testInstance);
                }
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void resolveOtherThanDocker(Optional<Object> testInstance)
            throws IllegalAccessException, MalformedURLException {
        Optional<Capabilities> capabilities = annotationsReader
                .getCapabilities(parameter, testInstance);

        Optional<URL> url;
        if (browser != null && browser.getUrl() != null
                && !browser.getUrl().isEmpty()) {
            url = Optional.of(new URL(browser.getUrl()));
            capabilities = Optional.of(new DesiredCapabilities(
                    browser.getType(), browser.getVersion(), ANY));
        } else {
            url = annotationsReader.getUrl(parameter, testInstance,
                    config.getSeleniumServerUrl());
        }

        if (url.isPresent() && capabilities.isPresent()) {
            object = resolveRemote(url.get(), capabilities.get());
        } else {
            object = resolveGeneric();
        }
    }

    @Override
    public String getName() {
        if (dockerDriverHandler != null) {
            return dockerDriverHandler.getName();
        } else {
            return super.getName();
        }
    }

    private WebDriver resolveRemote(URL url, Capabilities capabilities) {
        if (webDriverCreator == null) {
            webDriverCreator = new WebDriverCreator(getConfig());
        }
        return webDriverCreator.createRemoteWebDriver(url, capabilities);
    }

    private WebDriver resolveGeneric() {
        String defaultBrowser = getConfig().getDefaultBrowser();
        String defaultVersion = getConfig().getDefaultVersion();
        String defaultBrowserFallback = getConfig().getDefaultBrowserFallback();
        String defaultBrowserFallbackVersion = getConfig()
                .getDefaultBrowserFallbackVersion();
        String separator = ",";

        List<String> browserCandidates = new ArrayList<>();
        List<String> versionCandidates = new ArrayList<>();
        browserCandidates.add(defaultBrowser);
        versionCandidates.add(defaultVersion);

        if (defaultBrowserFallback.contains(separator)) {
            browserCandidates
                    .addAll(asList(defaultBrowserFallback.split(separator)));
        }
        if (defaultBrowserFallbackVersion.contains(separator)) {
            versionCandidates.addAll(
                    asList(defaultBrowserFallbackVersion.split(separator)));
        }
        assert browserCandidates.size() == versionCandidates
                .size() : "Number of browser and versions for fallback does not match";

        Iterator<String> browserIterator = browserCandidates.iterator();
        Iterator<String> versionIterator = versionCandidates.iterator();

        do {
            if (!browserIterator.hasNext()) {
                throw new SeleniumJupiterException(
                        "Browser candidate not found");
            }
            String browserCandidate = browserIterator.next();
            String versionCandidate = versionIterator.next();

            Browser candidate = new Browser(browserCandidate, versionCandidate);
            log.debug("Using generic handler, trying with {}",
                    browserCandidate);
            parent.putBrowserList(context.getUniqueId(),
                    singletonList(candidate));
            try {
                object = parent.resolveParameter(parameterContext, context);
            } catch (Exception e) {
                log.debug("There was an error with {} {}", browserCandidate,
                        e.getMessage());
                object = null;
            }
        } while (object == null);
        return (WebDriver) object;
    }

    @Override
    public void cleanup() {
        if (dockerDriverHandler != null) {
            dockerDriverHandler.cleanup();
        }
    }

    public void setParent(SeleniumExtension parent) {
        this.parent = parent;
    }

    public void setParameterContext(ParameterContext parameterContext) {
        this.parameterContext = parameterContext;
    }

}
