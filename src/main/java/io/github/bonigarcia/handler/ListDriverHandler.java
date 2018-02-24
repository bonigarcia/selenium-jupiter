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

import static io.github.bonigarcia.SeleniumJupiter.getInt;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

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
import org.openqa.selenium.remote.RemoteWebDriver;

import com.spotify.docker.client.exceptions.DockerCertificateException;

import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumJupiterException;

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
            Optional<Object> testInstance = context.getTestInstance();
            ParameterizedType parameterizedType = (ParameterizedType) parameter
                    .getParameterizedType();
            Type[] actualTypeArguments = parameterizedType
                    .getActualTypeArguments();
            if (actualTypeArguments[0] != RemoteWebDriver.class) {
                throw new SeleniumJupiterException(
                        "Invalid type of argument " + parameterizedType
                                + " (expected List<RemoteWebDriver>)");
            }

            Optional<DockerBrowser> dockerBrowser = annotationsReader
                    .getDocker(parameter);

            if (dockerBrowser.isPresent()) {
                final List<RemoteWebDriver> driverList = new CopyOnWriteArrayList<>();
                int numBrowsers = dockerBrowser.get().size();
                CountDownLatch latch = new CountDownLatch(numBrowsers);

                DockerDriverHandler firstDockerDriverHandler = new DockerDriverHandler(
                        context, parameter, testInstance, annotationsReader,
                        containerMap, dockerService, selenoidConfig, "_0");
                firstDockerDriverHandler.startSelenoidContainer();
                firstDockerDriverHandler.startNoVncContainer();
                containerMap = firstDockerDriverHandler.getContainerMap();

                executorService = newFixedThreadPool(numBrowsers);
                for (int i = 0; i < numBrowsers; i++) {
                    final int index = i;
                    executorService.submit(() -> resolveDockerBrowser(
                            firstDockerDriverHandler, testInstance,
                            dockerBrowser.get(), driverList, latch, index));
                }
                int timeout = numBrowsers
                        * getInt("sel.jup.docker.wait.timeout.sec");
                if (!latch.await(timeout, SECONDS)) {
                    throw new SeleniumJupiterException("Timeout of " + timeout
                            + " seconds waiting for start " + numBrowsers
                            + " dockerized browsers");
                }
                object = driverList;

            } else {
                log.warn("Annotation @DockerBrowser should be declared");
            }

        } catch (Exception e) {
            handleException(e);
        }
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
                            selenoidConfig, "_" + index);
            dockerDriverHandlerList.add(dockerDriverHandler);
            driverList.add((RemoteWebDriver) dockerDriverHandler
                    .resolve(dockerBrowser));
        } catch (DockerCertificateException e) {
            log.error("Exception in docker handler", e);
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
