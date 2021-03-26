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

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.dockerjava.api.exception.DockerException;
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

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.BrowserInstance;
import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiterException;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Resolver for lists of RemoteWebDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class ListDriverHandler extends DriverHandler {

    private List<DockerDriverHandler> dockerDriverHandlerList = new ArrayList<>();
    private ExecutorService executorService;

    public ListDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader) {
        super(parameter, context, config, annotationsReader);
    }

    @Override
    public void resolve() {
        try {
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
                DockerBrowser browser = dockerBrowser.get();
                resolveBrowserList(testInstance, browser);

            } else {
                log.warn("Annotation @DockerBrowser should be declared");
            }

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            handleException(e);
        }
    }

    private void resolveBrowserList(Optional<Object> testInstance,
            DockerBrowser dockerBrowser)
            throws DockerException, InterruptedException {
        List<RemoteWebDriver> driverList = new CopyOnWriteArrayList<>();
        int numBrowsers = dockerBrowser.size();
        CountDownLatch latch = new CountDownLatch(numBrowsers);
        BrowserInstance browserInstance = new BrowserInstance(config,
                annotationsReader, dockerBrowser.type(), dockerBrowser.cloud(),
                Optional.ofNullable(dockerBrowser.browserName()),
                Optional.ofNullable(dockerBrowser.volumes()));
        String version = dockerBrowser.version();

        String url = dockerBrowser.url();
        if (url != null && !url.isEmpty()) {
            dockerService.updateDockerClient(url);
        }

        DockerDriverHandler firstDockerDriverHandler = new DockerDriverHandler(
                context, parameter, testInstance, annotationsReader,
                containerMap, dockerService, config, browserInstance, version);
        firstDockerDriverHandler.setIndex("_0");
        firstDockerDriverHandler.startSelenoidContainer();
        if (getConfig().isVnc()) {
            firstDockerDriverHandler.startNoVncContainer();
        }
        containerMap = firstDockerDriverHandler.getContainerMap();

        boolean browserListInParallel = getConfig().isBrowserListInParallel();
        if (browserListInParallel) {
            executorService = newFixedThreadPool(numBrowsers);
        }
        for (int i = 0; i < numBrowsers; i++) {
            if (browserListInParallel) {
                final int index = i;
                executorService.submit(() -> resolveDockerBrowser(
                        firstDockerDriverHandler, testInstance, browserInstance,
                        dockerBrowser, driverList, latch, index));
            } else {
                resolveDockerBrowser(firstDockerDriverHandler, testInstance,
                        browserInstance, dockerBrowser, driverList, latch, i);
            }
        }
        int timeout = numBrowsers * getConfig().getDockerWaitTimeoutSec();
        if (!latch.await(timeout, SECONDS)) {
            throw new SeleniumJupiterException(
                    "Timeout of " + timeout + " seconds waiting for start "
                            + numBrowsers + " dockerized browsers");
        }
        object = driverList;
    }

    private void resolveDockerBrowser(
            DockerDriverHandler firstDockerDriverHandler,
            Optional<Object> testInstance, BrowserInstance browserInstance,
            DockerBrowser dockerBrowser, final List<RemoteWebDriver> driverList,
            CountDownLatch latch, int index) {
        try {
            DockerDriverHandler dockerDriverHandler = index == 0
                    ? firstDockerDriverHandler
                    : new DockerDriverHandler(context, parameter, testInstance,
                            annotationsReader, containerMap, dockerService,
                            config, browserInstance, dockerBrowser.version());
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
