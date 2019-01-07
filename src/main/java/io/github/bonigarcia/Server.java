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
package io.github.bonigarcia;

import static io.github.bonigarcia.BrowserType.OPERA;
import static io.github.bonigarcia.BrowserType.valueOf;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;

import com.google.gson.Gson;

import io.github.bonigarcia.config.Config;
import io.github.bonigarcia.handler.DockerDriverHandler;
import io.javalin.Handler;
import io.javalin.Javalin;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Selenium-Jupiter server.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 3.0.0
 */
public class Server {

    public static final MediaType JSON = MediaType
            .parse("application/json; charset=utf-8");
    public static final String GET = "GET";
    public static final String DELETE = "DELETE";
    public static final String POST = "POST";
    public static final String SESSION = "/session";

    final Logger log = getLogger(lookup().lookupClass());

    public Server(int port) {
        Javalin app = Javalin.create().start(port);
        Config config = new Config();
        InternalPreferences preferences = new InternalPreferences(config);
        Gson gson = new Gson();
        final String[] hubUrl = new String[1];
        final DockerDriverHandler[] dockerDriverHandler = new DockerDriverHandler[1];

        Handler handler = ctx -> {
            String requestMethod = ctx.method();
            String requestPath = ctx.path();
            String requestBody = ctx.body();
            log.info("Server request: {} {}", requestMethod, requestPath);
            log.debug("body: {} ", requestBody);

            Session session = gson.fromJson(requestBody, Session.class);

            // POST /session
            if (session != null && session.getDesiredCapabilities() != null) {
                String browserName = session.getDesiredCapabilities()
                        .getBrowserName();
                String version = session.getDesiredCapabilities().getVersion();
                BrowserType browserType = browserName
                        .equalsIgnoreCase("operablink") ? OPERA
                                : valueOf(browserName.toUpperCase());
                BrowserInstance browserInstance = new BrowserInstance(config,
                        browserType);
                dockerDriverHandler[0] = new DockerDriverHandler(config,
                        browserInstance, version, preferences);

                dockerDriverHandler[0].resolve(browserInstance, version, "", "",
                        false);
                hubUrl[0] = dockerDriverHandler[0].getHubUrl().toString();
                log.info("Hub URL {}", hubUrl[0]);
            }

            // exchange request-response
            String response = exchange(hubUrl[0] + requestPath, requestMethod,
                    requestBody);
            log.info("Server response: {}", response);
            ctx.result(response);

            // DELETE /session/sessionId
            if (requestMethod.equalsIgnoreCase(DELETE)
                    && requestPath.startsWith(SESSION + "/")) {
                dockerDriverHandler[0].cleanup();
            }
        };

        app.post(SESSION, handler);
        app.post(SESSION + "/*", handler);
        app.get(SESSION + "/*", handler);
        app.delete(SESSION + "/*", handler);

        log.info("Selenium-Jupiter server listening on port {}", port);
    }

    public static String exchange(String url, String method, String json)
            throws IOException {
        OkHttpClient client = new OkHttpClient();
        Builder requestBuilder = new Request.Builder().url(url);
        switch (method) {
        case GET:
            requestBuilder.get();
            break;
        case DELETE:
            requestBuilder.delete();
            break;
        default:
        case POST:
            RequestBody body = RequestBody.create(JSON, json);
            requestBuilder.post(body);
            break;
        }
        Response response = client.newCall(requestBuilder.build()).execute();
        return response.body().string();
    }

    static class Session {
        DesiredCapabilities desiredCapabilities;

        public DesiredCapabilities getDesiredCapabilities() {
            return desiredCapabilities;
        }
    }

    static class DesiredCapabilities {
        String browserName;
        String version;
        String platform;

        public String getBrowserName() {
            return browserName;
        }

        public String getVersion() {
            return version;
        }

        public String getPlatform() {
            return platform;
        }
    }

}