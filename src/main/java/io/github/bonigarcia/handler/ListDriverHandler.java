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

import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.DockerBrowser;

/**
 * Resolver for lists of RemoteWebDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class ListDriverHandler extends DriverHandler {

    private DockerDriverHandler dockerDriverHandler;

    private ExecutorService executorService;

    public ListDriverHandler(Parameter parameter,
            Optional<Object> testInstance) {
        super(parameter, testInstance);
    }

    @Override
    public List<RemoteWebDriver> resolve() {
        try {
            Optional<DockerBrowser> dockerBrowser = annotationsReader
                    .getDocker(parameter);

            if (dockerBrowser.isPresent()) {
                dockerDriverHandler = new DockerDriverHandler();
                int numBrowsers = dockerBrowser.get().size();
                final List<RemoteWebDriver> driverList = new CopyOnWriteArrayList<>();

                executorService = newFixedThreadPool(numBrowsers);
                CountDownLatch latch = new CountDownLatch(numBrowsers);
                for (int i = 0; i < numBrowsers; i++) {
                    executorService.submit(() -> {
                        try {
                            driverList.add((RemoteWebDriver) dockerDriverHandler
                                    .resolve(dockerBrowser.get(), parameter,
                                            testInstance, annotationsReader));
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                latch.await(numBrowsers * 2, SECONDS);
                return driverList;

            } else {
                log.warn("Annotation @DockerBrowser should be declared");
            }

        } catch (Exception e) {
            handleException(e);
        }

        return emptyList();
    }

    @Override
    public void cleanup() {
        if (dockerDriverHandler != null) {
            dockerDriverHandler.cleanup();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

}
