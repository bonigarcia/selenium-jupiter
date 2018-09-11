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

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;

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
    public static final String HUB = "http://localhost:9515";

    final Logger log = getLogger(lookup().lookupClass());

    public Server(int port) {
        Javalin app = Javalin.create().start(port);
        Handler handler = ctx -> {
            String requestMethod = ctx.method();
            String requestPath = ctx.path();
            String requestBody = ctx.body();
            log.info("Server request: {} {}", requestMethod, requestPath);
            String response = exchange(HUB + requestPath, requestMethod,
                    requestBody);
            log.info("Server response: {}", response);
        };

        app.post("/session", handler);
        app.post("/session/*", handler);
        app.get("/session/*", handler);
        app.delete("/session/*", handler);

        log.info("Selenium-Jupiter server listening on port {}", port);
    }

    public static String exchange(String url, String method, String json)
            throws IOException {
        OkHttpClient client = new OkHttpClient();
        Builder requestBuilder = new Request.Builder().url(url);
        switch (method) {
        case "GET":
            requestBuilder.get();
            break;
        case "DELETE":
            requestBuilder.delete();
            break;
        default:
        case "POST":
            RequestBody body = RequestBody.create(JSON, json);
            requestBuilder.post(body);
            break;
        }
        Response response = client.newCall(requestBuilder.build()).execute();
        return response.body().string();
    }

    public static void main(String[] args) {
        new Server(4042);
    }

}