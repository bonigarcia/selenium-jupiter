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

import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.BrowsersTemplate.Browser;
import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumJupiterException;

/**
 * Resolver for RemoteWebDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class RemoteDriverHandler extends DriverHandler {

    private DockerDriverHandler dockerDriverHandler;
    private Browser browser;

    public RemoteDriverHandler(Parameter parameter, ExtensionContext context) {
        super(parameter, context);
    }

    public RemoteDriverHandler(Parameter parameter, ExtensionContext context,
            Browser browser) {
        super(parameter, context);
        this.browser = browser;
    }

    @Override
    public WebDriver resolve() {
        WebDriver driver = null;
        try {
            Optional<Object> testInstance = context.getTestInstance();
            dockerDriverHandler = new DockerDriverHandler(context, parameter,
                    testInstance, annotationsReader);

            if (browser != null) {
                driver = dockerDriverHandler.resolve(browser.toBrowserType(),
                        browser.getVersion());
            } else {
                Optional<DockerBrowser> dockerBrowser = annotationsReader
                        .getDocker(parameter);

                if (dockerBrowser.isPresent()) {
                    driver = dockerDriverHandler.resolve(dockerBrowser.get());
                } else {
                    driver = resolveRemote();
                }
            }
        } catch (Exception e) {
            handleException(e);
        }
        return driver;
    }

    private WebDriver resolveRemote()
            throws IllegalAccessException, MalformedURLException {
        WebDriver driver = null;
        Optional<Object> testInstance = context.getTestInstance();
        Optional<Capabilities> capabilities = annotationsReader
                .getCapabilities(parameter, testInstance);

        Optional<URL> url = annotationsReader.getUrl(parameter, testInstance);
        if (url.isPresent() && capabilities.isPresent()) {
            driver = new RemoteWebDriver(url.get(), capabilities.get());
        } else {
            String urlMessage = url.isPresent() ? "" : "URL not present ";
            String noCapsMessage = capabilities.isPresent() ? ""
                    : "Capabilites not present";
            String errMessage = "Was not possible to instantiate RemoteWebDriver: "
                    + urlMessage + noCapsMessage;
            if (throwExceptionWhenNoDriver()) {
                throw new SeleniumJupiterException(errMessage);
            } else {
                log.warn(errMessage);
            }
        }
        return driver;
    }

    @Override
    public void cleanup() {
        if (dockerDriverHandler != null) {
            dockerDriverHandler.cleanup();
        }
    }

}
