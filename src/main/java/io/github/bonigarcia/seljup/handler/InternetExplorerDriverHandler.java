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
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ie.InternetExplorerOptions;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.Options;
import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Handler for Internet Explorer.
 *
 * @author Boni Garcia
 * @since 3.2.0
 */
public class InternetExplorerDriverHandler extends DriverHandler {

    public InternetExplorerDriverHandler(Parameter parameter,
            ExtensionContext context, Config config,
            AnnotationsReader annotationsReader, Optional<Browser> browser) {
        super(parameter, context, config, annotationsReader, browser);
    }

    @Override
    public Capabilities getOptions(Parameter parameter,
            Optional<Object> testInstance) {
        InternetExplorerOptions internetExplorerOptions = new InternetExplorerOptions();
        InternetExplorerOptions optionsFromAnnotatedField = annotationsReader
                .getFromAnnotatedField(testInstance, Options.class,
                        InternetExplorerOptions.class);
        if (optionsFromAnnotatedField != null) {
            internetExplorerOptions = optionsFromAnnotatedField;
        }
        return internetExplorerOptions;
    }

}
