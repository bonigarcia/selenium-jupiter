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

import static io.github.bonigarcia.seljup.CloudType.NONE;

import java.util.List;
import java.util.stream.Stream;

/**
 * Template browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class BrowsersTemplate {

    static final String IN_DOCKER = "-in-docker";

    List<List<Browser>> browsers;

    public Stream<List<Browser>> getStream() {
        return browsers.stream();
    }

    public static class Browser {
        String type;
        String version;
        String browserName;
        String deviceName;
        String url;
        String cloud;
        String[] volumes;

        public Browser(String type, String version, String browserName,
                String deviceName, String url, String cloud, String[] volumes) {
            this.type = type;
            this.version = version;
            this.browserName = browserName;
            this.deviceName = deviceName;
            this.url = url;
            this.cloud = cloud;
            this.volumes = volumes;
        }

        public Browser(String type, String version) {
            this.type = type;
            this.version = version;
        }

        public Browser(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public String getVersion() {
            return version;
        }

        public String getVersionForRemote() {
            if (version != null && version.equalsIgnoreCase("latest")) {
                return "";
            }
            return version;
        }

        public String getBrowserName() {
            return browserName;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCloud() {
            return cloud;
        }

        public void setCloud(String cloud) {
            this.cloud = cloud;
        }

        public BrowserType toBrowserType() {
            return BrowserType
                    .valueOf(getType().replace(IN_DOCKER, "").toUpperCase());

        }

        public CloudType toCloudType() {
            if (getCloud() == null || getCloud().isEmpty()) {
                return NONE;
            } else {
                return CloudType.valueOf(getCloud().toUpperCase());
            }

        }

        public boolean isDockerBrowser() {
            return getType().contains(IN_DOCKER);
        }

        public String[] getVolumes() {
            return volumes;
        }

        public void setVolumes(String[] volumes) {
            this.volumes = volumes;
        }

        @Override
        public String toString() {
            String versionMessage = getVersion() != null
                    ? ", version=" + getVersion()
                    : "";
            return "Browser [type=" + getType() + versionMessage + "]";
        }

    }

}
