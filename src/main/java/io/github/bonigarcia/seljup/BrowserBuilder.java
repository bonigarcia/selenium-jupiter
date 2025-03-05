/*
 * (C) Copyright 2018 Boni Garcia (https://bonigarcia.github.io/)
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

import com.google.gson.internal.LinkedTreeMap;

import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;

/**
 * Browser builder.
 *
 * @author Boni Garcia
 * @since 2.2.0
 */
public class BrowserBuilder {

    String type;
    String version;
    String remoteUrl;
    String binary;
    String[] arguments;
    String[] preferences;
    Object capabilities;

    public BrowserBuilder(String type) {
        this.type = type;
    }

    public static BrowserBuilder chrome() {
        return new BrowserBuilder("chrome");
    }

    public static BrowserBuilder firefox() {
        return new BrowserBuilder("firefox");
    }

    public static BrowserBuilder edge() {
        return new BrowserBuilder("edge");
    }

    public static BrowserBuilder opera() {
        return new BrowserBuilder("opera");
    }

    public static BrowserBuilder safari() {
        return new BrowserBuilder("safari");
    }

    public static BrowserBuilder iexplorer() {
        return new BrowserBuilder("iexplorer");
    }

    public static BrowserBuilder chromeInDocker() {
        return new BrowserBuilder("chrome-in-docker");
    }

    public static BrowserBuilder firefoxInDocker() {
        return new BrowserBuilder("firefox-in-docker");
    }

    public static BrowserBuilder chromiumInDocker() {
        return new BrowserBuilder("chromium-in-docker");
    }

    public static BrowserBuilder edgeInDocker() {
        return new BrowserBuilder("edge-in-docker");
    }

    public BrowserBuilder version(String version) {
        this.version = version;
        return this;
    }

    public BrowserBuilder remoteUrl(String url) {
        this.remoteUrl = url;
        return this;
    }

    public BrowserBuilder arguments(String[] arguments) {
        this.arguments = arguments;
        return this;
    }

    public BrowserBuilder preferences(String[] preferences) {
        this.preferences = preferences;
        return this;
    }

    public BrowserBuilder capabilities(
            LinkedTreeMap<String, String> capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public Browser build() {
        return new Browser(type, version, remoteUrl, binary, arguments,
                preferences, capabilities);
    }

}
