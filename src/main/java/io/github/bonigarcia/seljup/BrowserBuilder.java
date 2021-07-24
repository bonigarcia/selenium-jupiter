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
    String[] arguments;
    Object capabilities;

    private BrowserBuilder(String type) {
        this.type = type;
    }

    public static BrowserBuilder chrome() {
        return new BrowserBuilder("chrome");
    }

    public static BrowserBuilder chromeMobile() {
        return new BrowserBuilder("chrome-mobile");
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

    public static BrowserBuilder operaInDocker() {
        return new BrowserBuilder("opera-in-docker");
    }

    public static BrowserBuilder edgeInDocker() {
        return new BrowserBuilder("edge-in-docker");
    }

    public static BrowserBuilder safariInDocker() {
        return new BrowserBuilder("safari-in-docker");
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

    public BrowserBuilder capabilities(Object capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public Browser build() {
        return new Browser(type, version, remoteUrl, arguments, capabilities);
    }

}
