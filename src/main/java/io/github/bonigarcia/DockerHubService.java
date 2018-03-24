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

import static io.github.bonigarcia.SeleniumJupiter.config;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;

import io.github.bonigarcia.DockerHubTags.DockerHubTag;
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
public class DockerHubService {

    final Logger log = getLogger(lookup().lookupClass());

    DockerHubApi dockerHubApi;

    public DockerHubService() {
        String dockerHubUrl = config().getDockerHubUrl();
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(dockerHubUrl).build();
        dockerHubApi = retrofit.create(DockerHubApi.class);
    }

    public List<DockerHubTag> listTags() throws IOException {
        log.info("Getting browser image list from Docker Hub");
        Response<DockerHubTags> listTagsResponse = dockerHubApi.listTags()
                .execute();
        if (!listTagsResponse.isSuccessful()) {
            throw new SeleniumJupiterException(
                    listTagsResponse.errorBody().string());
        }
        return listTagsResponse.body().getResults();
    }

}