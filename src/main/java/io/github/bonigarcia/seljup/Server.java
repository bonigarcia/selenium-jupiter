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

import static io.github.bonigarcia.seljup.BrowserType.EDGE;
import static io.github.bonigarcia.seljup.BrowserType.IEXPLORER;
import static io.github.bonigarcia.seljup.BrowserType.OPERA;
import static io.github.bonigarcia.seljup.BrowserType.valueOf;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;

import com.google.gson.Gson;

import io.github.bonigarcia.seljup.config.Config;
import io.github.bonigarcia.seljup.handler.DockerDriverHandler;
import io.javalin.Javalin;
import io.javalin.http.Handler;
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
        AnnotationsReader annotationsReader = new AnnotationsReader();
        DockerCache dockerCache = new DockerCache(config);
        Gson gson = new Gson();
        final String[] hubUrl = new String[1];
        final DockerDriverHandler[] dockerDriverHandler = new DockerDriverHandler[1];
        String path = config.getServerPath();
        final String serverPath = path.endsWith("/")
                ? path.substring(0, path.length() - 1)
                : path;
        final int timeoutSec = config.getServerTimeoutSec();

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
                BrowserType browserType = getBrowserType(browserName);
                BrowserInstance browserInstance = new BrowserInstance(config,
                        annotationsReader, browserType, empty(), empty());
                dockerDriverHandler[0] = new DockerDriverHandler(config,
                        browserInstance, version, dockerCache);

                dockerDriverHandler[0].resolve(browserInstance, version, "", "",
                        false);
                hubUrl[0] = dockerDriverHandler[0].getHubUrl().toString();
                log.info("Hub URL {}", hubUrl[0]);
            }

            // exchange request-response
            String response = exchange(
                    hubUrl[0] + requestPath.replace(serverPath, ""),
                    requestMethod, requestBody, timeoutSec);
            log.info("Server response: {}", response);
            ctx.result(response);

            // DELETE /session/sessionId
            if (requestMethod.equalsIgnoreCase(DELETE)
                    && requestPath.startsWith(serverPath + SESSION + "/")) {
                dockerDriverHandler[0].cleanup();
            }
        };

        app.post(serverPath + SESSION, handler);
        app.post(serverPath + SESSION + "/*", handler);
        app.get(serverPath + SESSION + "/*", handler);
        app.delete(serverPath + SESSION + "/*", handler);

        String serverUrl = String.format("http://localhost:%d%s", port,
                serverPath);
        log.info("Selenium-Jupiter server listening on {}", serverUrl);
    }

    public BrowserType getBrowserType(String browserName) {
        BrowserType browserType;
        switch (browserName) {
        case "operablink":
            browserType = OPERA;
            break;
        case "MicrosoftEdge":
            browserType = EDGE;
            break;
        case "internet explorer":
            browserType = IEXPLORER;
            break;
        default:
            browserType = valueOf(browserName.toUpperCase());
            break;
        }
        return browserType;
    }

    public static String exchange(String url, String method, String json,
            int timeoutSec) throws IOException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(timeoutSec, SECONDS);
        builder.readTimeout(timeoutSec, SECONDS);
        builder.writeTimeout(timeoutSec, SECONDS);
        OkHttpClient client = builder.build();

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