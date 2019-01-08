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

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.Options;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Handler for Edge.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class EdgeDriverHandler extends DriverHandler {

    public EdgeDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader) {
        super(parameter, context, config, annotationsReader);
    }

    @Override
    public void resolve() {
        try {
            Optional<Object> testInstance = context.getTestInstance();
            Optional<Capabilities> capabilities = annotationsReader
                    .getCapabilities(parameter, testInstance);
            EdgeOptions edgeOptions = (EdgeOptions) getOptions(parameter,
                    testInstance);
            if (capabilities.isPresent()) {
                edgeOptions.merge(capabilities.get());
            }
            object = new EdgeDriver(edgeOptions);
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    public MutableCapabilities getOptions(Parameter parameter,
            Optional<Object> testInstance)
            throws IOException, IllegalAccessException {
        EdgeOptions edgeOptions = new EdgeOptions();
        EdgeOptions optionsFromAnnotatedField = annotationsReader
                .getOptionsFromAnnotatedField(testInstance, Options.class,
                        EdgeOptions.class);
        if (optionsFromAnnotatedField != null) {
            edgeOptions = optionsFromAnnotatedField;
        }
        return edgeOptions;
    }

}
