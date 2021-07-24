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

import static java.util.Arrays.stream;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.edge.EdgeOptions;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.Arguments;
import io.github.bonigarcia.seljup.Binary;
import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import io.github.bonigarcia.seljup.Extensions;
import io.github.bonigarcia.seljup.Options;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Handler for Edge.
 *
 * @author Boni Garcia
 * @since 2.0.0
 */
public class EdgeDriverHandler extends DriverHandler {

    public EdgeDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader,
            Optional<Browser> browser) {
        super(parameter, context, config, annotationsReader, browser);
    }

    @Override
    public Capabilities getOptions(Parameter parameter,
            Optional<Object> testInstance) {
        EdgeOptions options = new EdgeOptions();
        if (parameter != null) {
            // @Arguments
            Arguments arguments = parameter.getAnnotation(Arguments.class);
            if (arguments != null) {
                stream(arguments.value()).forEach(options::addArguments);
            }
            if (browser.isPresent() && browser.get() != null
                    && browser.get().getArguments() != null) {
                Arrays.stream(browser.get().getArguments())
                        .forEach(options::addArguments);
            }

            // @Extensions
            Extensions extensions = parameter.getAnnotation(Extensions.class);
            if (extensions != null) {
                for (String extension : extensions.value()) {
                    options.addExtensions(getExtension(extension));
                }
            }

            // @Binary
            Binary binary = parameter.getAnnotation(Binary.class);
            if (binary != null) {
                options.setBinary(binary.value());
            }

            // @Options
            EdgeOptions optionsFromAnnotatedField = annotationsReader
                    .getFromAnnotatedField(testInstance, Options.class,
                            EdgeOptions.class);
            if (optionsFromAnnotatedField != null) {
                options = optionsFromAnnotatedField.merge(options);
            }
        }
        return options;
    }

}
