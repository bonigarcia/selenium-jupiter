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
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.spotify.docker.client.exceptions.DockerException;

import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.SelenoidConfig;

/**
 * Resolver for lists of RemoteWebDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class ListDriverHandler extends DriverHandler {

    private List<DockerDriverHandler> dockerDriverHandlerList = new ArrayList<>();
    private ExecutorService executorService;

    public ListDriverHandler(Parameter parameter, ExtensionContext context) {
        super(parameter, context);
    }

    @Override
    public void resolve() {
        try {
            if (selenoidConfig == null) {
                selenoidConfig = new SelenoidConfig();
            }

            Optional<Object> testInstance = context.getTestInstance();
            ParameterizedType parameterizedType = (ParameterizedType) parameter
                    .getParameterizedType();
            Type[] actualTypeArguments = parameterizedType
                    .getActualTypeArguments();
            if (actualTypeArguments[0] != RemoteWebDriver.class
                    && actualTypeArguments[0] != WebDriver.class) {
                throw new SeleniumJupiterException("Invalid type of argument "
                        + parameterizedType
                        + " (expected List<RemoteWebDriver> or List<WebDriver>)");
            }

            Optional<DockerBrowser> dockerBrowser = annotationsReader
                    .getDocker(parameter);

            if (dockerBrowser.isPresent()) {
                resolveBrowserList(testInstance, dockerBrowser.get());

            } else {
                log.warn("Annotation @DockerBrowser should be declared");
            }

        } catch (Exception e) {
            handleException(e);
        }
    }

    private void resolveBrowserList(Optional<Object> testInstance,
            DockerBrowser dockerBrowser)
            throws DockerException, InterruptedException, IOException {
        List<RemoteWebDriver> driverList = new CopyOnWriteArrayList<>();
        int numBrowsers = dockerBrowser.size();
        CountDownLatch latch = new CountDownLatch(numBrowsers);

        DockerDriverHandler firstDockerDriverHandler = new DockerDriverHandler(
                context, parameter, testInstance, annotationsReader,
                containerMap, dockerService, selenoidConfig);
        firstDockerDriverHandler.setIndex("_0");
        firstDockerDriverHandler.startSelenoidContainer();
        if (config().isVnc()) {
            firstDockerDriverHandler.startNoVncContainer();
        }
        containerMap = firstDockerDriverHandler.getContainerMap();

        boolean browserListInParallel = config().isBrowserListInParallel();
        if (browserListInParallel) {
            executorService = newFixedThreadPool(numBrowsers);
        }
        for (int i = 0; i < numBrowsers; i++) {
            if (browserListInParallel) {
                final int index = i;
                executorService.submit(() -> resolveDockerBrowser(
                        firstDockerDriverHandler, testInstance, dockerBrowser,
                        driverList, latch, index));
            } else {
                resolveDockerBrowser(firstDockerDriverHandler, testInstance,
                        dockerBrowser, driverList, latch, i);
            }
        }
        int timeout = numBrowsers * config().getDockerWaitTimeoutSec();
        if (!latch.await(timeout, SECONDS)) {
            throw new SeleniumJupiterException(
                    "Timeout of " + timeout + " seconds waiting for start "
                            + numBrowsers + " dockerized browsers");
        }
        object = driverList;
    }

    private void resolveDockerBrowser(
            DockerDriverHandler firstDockerDriverHandler,
            Optional<Object> testInstance, DockerBrowser dockerBrowser,
            final List<RemoteWebDriver> driverList, CountDownLatch latch,
            int index) {
        try {
            DockerDriverHandler dockerDriverHandler = index == 0
                    ? firstDockerDriverHandler
                    : new DockerDriverHandler(context, parameter, testInstance,
                            annotationsReader, containerMap, dockerService,
                            selenoidConfig);
            dockerDriverHandler.setIndex("_" + index);
            dockerDriverHandlerList.add(dockerDriverHandler);
            driverList.add((RemoteWebDriver) dockerDriverHandler
                    .resolve(dockerBrowser));
        } finally {
            latch.countDown();
        }
    }

    @Override
    public void cleanup() {
        if (!dockerDriverHandlerList.isEmpty()) {
            dockerDriverHandlerList.forEach(DockerDriverHandler::cleanup);
            dockerDriverHandlerList.clear();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

}
