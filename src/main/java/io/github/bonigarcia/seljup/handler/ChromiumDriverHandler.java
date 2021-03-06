/*
 * (C) Copyright 2021 Boni Garcia (http://bonigarcia.github.io/)
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

import static org.openqa.selenium.net.PortProber.findFreePort;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.config.Config;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Resolver for ChromiumDriver.
 *
 * @author Boni Garcia
 * @since 4.0.0
 */
public class ChromiumDriverHandler extends ChromeDriverHandler {

    public ChromiumDriverHandler(Config config,
            AnnotationsReader annotationsReader) {
        super(config, annotationsReader);
    }

    public ChromiumDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader) {
        super(parameter, context, config, annotationsReader);
    }

    @Override
    public MutableCapabilities getOptions(Parameter parameter,
            Optional<Object> testInstance)
            throws IOException, IllegalAccessException {
        ChromeOptions chromeOptions = (ChromeOptions) super.getOptions(
                parameter, testInstance);
        Optional<Path> browserPath = WebDriverManager.chromiumdriver()
                .getBrowserPath();
        if (browserPath.isPresent()) {
            chromeOptions.setBinary(browserPath.get().toString());
        }
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--remote-debugging-port=" + findFreePort());
        return chromeOptions;
    }

}
