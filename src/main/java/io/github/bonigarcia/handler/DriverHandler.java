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

import static io.github.bonigarcia.SeleniumJupiter.getBoolean;
import static org.slf4j.LoggerFactory.getLogger;
import static java.lang.invoke.MethodHandles.lookup;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.SeleniumJupiterException;

/**
 * Abstract resolver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public abstract class DriverHandler {

    final Logger log = getLogger(lookup().lookupClass());

    boolean exceptionWhenNoDriver = getBoolean(
            "sel.jup.exception.when.no.driver");

    public abstract WebDriver resolve(Parameter parameter,
            Optional<Object> testInstance);

    void handleException(Exception e) {
        if (throwExceptionWhenNoDriver()) {
            throw new SeleniumJupiterException(e);
        }
        log.warn("Error creating WebDriver object", e);
    }

    boolean throwExceptionWhenNoDriver() {
        return exceptionWhenNoDriver;
    }

}
