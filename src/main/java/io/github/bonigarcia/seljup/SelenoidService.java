/*
 * (C) Copyright 2018 Boni Garcia (http://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Optional;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Docker Hub service.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class SelenoidService {

    final Logger log = getLogger(lookup().lookupClass());

    static final String CONTAINER = "container=";

    SelenoidApi selenoidApi;

    public SelenoidService(String selenoidUrl) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(selenoidUrl).build();
        selenoidApi = retrofit.create(SelenoidApi.class);
    }

    public Optional<String> getContainerId(WebDriver driver)
            throws IOException {
        Optional<String> output = empty();
        Response<SelenoidStatus> execute = selenoidApi.status().execute();
        String browsers = execute.body().getBrowsers();

        String sessionId = ((RemoteWebDriver) driver).getSessionId().toString();
        int i = browsers.indexOf(sessionId);
        if (i != -1) {
            int j = browsers.indexOf(CONTAINER, i) + CONTAINER.length();
            int k = browsers.indexOf(',', j);
            if (j != -1 && k != -1) {
                String containerId = browsers.substring(j, k);
                output = Optional.of(containerId);
            }
        }

        log.trace("Container id for session {} = {}", sessionId, output);
        return output;
    }

}
