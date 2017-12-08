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

import static io.github.bonigarcia.SeleniumJupiter.PAGE_LOAD_STRATEGY;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;

import io.github.bonigarcia.AnnotationsReader;
import io.github.bonigarcia.DriverOptions;
import io.github.bonigarcia.Option;

/**
 * Resolver for ChromeDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class EdgeDriverHandler {

    final Logger log = getLogger(lookup().lookupClass());

    static EdgeDriverHandler instance;

    public static synchronized EdgeDriverHandler getInstance() {
        if (instance == null) {
            instance = new EdgeDriverHandler();
        }
        return instance;
    }

    public WebDriver resolve(Parameter parameter,
            Optional<Object> testInstance) {
        Optional<Capabilities> capabilities = AnnotationsReader.getInstance()
                .getCapabilities(parameter, testInstance);
        EdgeOptions edgeOptions = getEdgeOptions(parameter, testInstance);
        if (capabilities.isPresent()) {
            edgeOptions.merge(capabilities.get());
        }
        return new EdgeDriver(edgeOptions);
    }

    public EdgeOptions getEdgeOptions(Parameter parameter,
            Optional<Object> testInstance) {
        EdgeOptions edgeOptions = new EdgeOptions();
        DriverOptions driverOptions = parameter
                .getAnnotation(DriverOptions.class);

        // Search first DriverOptions annotation in parameter
        if (driverOptions != null) {
            for (Option option : driverOptions.options()) {
                String name = option.name();
                String value = option.value();

                if (name.equals(PAGE_LOAD_STRATEGY)) {
                    edgeOptions.setPageLoadStrategy(value);
                } else {
                    log.warn("Option {} not supported for Edge", name);
                }
            }
        } else {
            // If not, search DriverOptions in any field
            Object optionsFromAnnotatedField = AnnotationsReader.getInstance()
                    .getOptionsFromAnnotatedField(testInstance,
                            DriverOptions.class);
            if (optionsFromAnnotatedField != null) {
                edgeOptions = (EdgeOptions) optionsFromAnnotatedField;
            }
        }
        return edgeOptions;
    }

}
