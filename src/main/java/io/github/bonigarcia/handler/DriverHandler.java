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

import static io.github.bonigarcia.SeleniumJupiter.config;
import static java.io.File.createTempFile;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import io.github.bonigarcia.AnnotationsReader;
import io.github.bonigarcia.DockerContainer;
import io.github.bonigarcia.DockerService;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.SelenoidConfig;

/**
 * Abstract resolver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public abstract class DriverHandler {

    static final Logger log = getLogger(lookup().lookupClass());

    AnnotationsReader annotationsReader = new AnnotationsReader();
    Parameter parameter;
    ExtensionContext context;
    Map<String, DockerContainer> containerMap;
    DockerService dockerService;
    SelenoidConfig selenoidConfig;
    Object object;

    public abstract void resolve();

    public DriverHandler() {
        // Default constructor
    }

    public DriverHandler(Parameter parameter, ExtensionContext context) {
        this.parameter = parameter;
        this.context = context;
    }

    public Object getObject() {
        return object;
    }

    public String getName() {
        String name = "";
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isPresent()) {
            name = testMethod.get().getName();
        }
        name += "_" + parameter.getName() + "_"
                + object.getClass().getSimpleName();
        if (RemoteWebDriver.class.isAssignableFrom(object.getClass())) {
            name += "_" + ((RemoteWebDriver) object).getSessionId();
        }
        return name;
    }

    public boolean throwExceptionWhenNoDriver() {
        return config().isExceptionWhenNoDriver();
    }

    void handleException(Exception e) {
        if (throwExceptionWhenNoDriver()) {
            log.trace("Internal error in selenium-jupiter", e);
            throw new SeleniumJupiterException(e);
        }
        log.warn("Error creating WebDriver object", e);
    }

    File getExtension(String fileName) {
        File file = new File(fileName);
        try {
            if (!file.exists()) {
                InputStream inputStream = this.getClass()
                        .getResourceAsStream("/" + file);
                if (inputStream != null) {
                    file = createTempFile("tmp-", fileName);
                    file.deleteOnExit();
                    copyInputStreamToFile(inputStream, file);
                }
            }
        } catch (Exception e) {
            log.warn("There was a problem handling extension", e);
        }
        return file;
    }

    public MutableCapabilities getOptions(Parameter parameter,
            Optional<Object> testInstance)
            throws IOException, IllegalAccessException {
        throw new IllegalAccessException("Not implemented");
    }

    public void cleanup() {
        // Nothing by default
    }

    public void setContainerMap(Map<String, DockerContainer> containerMap) {
        this.containerMap = containerMap;
    }

    public void setDockerService(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    public void setSelenoidConfig(SelenoidConfig selenoidConfig) {
        this.selenoidConfig = selenoidConfig;
    }

}
