/*
 * (C) Copyright 2019 Boni Garcia (http://bonigarcia.github.io/)
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

import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.BrowserInstance;
import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SelenideConfiguration;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Resolver for SelenideDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 3.2.0
 */
public class SelenideDriverHandler extends DriverHandler {

    private DockerDriverHandler dockerDriverHandler;

    public SelenideDriverHandler(Config config,
            AnnotationsReader annotationsReader) {
        super(config, annotationsReader);
    }

    public SelenideDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader) {
        super(parameter, context, config, annotationsReader);
    }

    @Override
    public void resolve() {
        try {
            Optional<Object> testInstance = context.getTestInstance();
            Optional<DockerBrowser> dockerBrowser = annotationsReader
                    .getDocker(parameter);
            if (dockerBrowser.isPresent()) {
                BrowserInstance browserInstance = new BrowserInstance(config,
                        annotationsReader, dockerBrowser.get().type(),
                        Optional.ofNullable(dockerBrowser.get().browserName()),
                        Optional.ofNullable(dockerBrowser.get().volumes()));
                dockerDriverHandler = new DockerDriverHandler(context,
                        parameter, testInstance, annotationsReader,
                        containerMap, dockerService, config, browserInstance,
                        dockerBrowser.get().version());
                object = dockerDriverHandler.resolve(dockerBrowser.get());
            }

            SelenideConfig selenideConfig = getSelenideConfig(parameter,
                    testInstance);

            if (object != null) {
                WebDriver webdriver = (WebDriver) object;
                object = new SelenideDriver(selenideConfig, webdriver, null);
            } else {
                Optional<Capabilities> capabilities = annotationsReader
                        .getCapabilities(parameter, testInstance);
                Optional<URL> url = annotationsReader.getUrl(parameter,
                        testInstance, config.getSeleniumServerUrl());

                if (capabilities.isPresent() && url.isPresent()) {
                    selenideConfig.remote(url.get().toString());
                    DesiredCapabilities browserCapabilities = (DesiredCapabilities) capabilities
                            .get();
                    selenideConfig.browserCapabilities(browserCapabilities);
                    selenideConfig
                            .browser(browserCapabilities.getBrowserName());
                }

                object = new SelenideDriver(selenideConfig);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    public void cleanup() {
        if (dockerDriverHandler != null) {
            dockerDriverHandler.cleanup();
        }
    }

    public SelenideConfig getSelenideConfig(Parameter parameter,
            Optional<Object> testInstance) throws IllegalAccessException {

        SelenideConfig config = new SelenideConfig();
        if (parameter != null) {
            // @SelenideConfiguration as parameter
            SelenideConfiguration selenideConfiguration = parameter
                    .getAnnotation(SelenideConfiguration.class);
            if (selenideConfiguration != null) {
                config.browser(selenideConfiguration.browser());
                config.headless(selenideConfiguration.headless());
                config.browserBinary(selenideConfiguration.browserBinary());
            }

            // @SelenideConfiguration as field
            SelenideConfig globalConfig = annotationsReader
                    .getFromAnnotatedField(testInstance,
                            SelenideConfiguration.class, SelenideConfig.class);
            if (globalConfig != null) {
                config = globalConfig;
            }
        }

        return config;
    }

}
