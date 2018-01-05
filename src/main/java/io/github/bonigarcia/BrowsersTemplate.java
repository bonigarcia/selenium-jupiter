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

import static io.github.bonigarcia.BrowserType.valueOf;

import java.util.List;
import java.util.stream.Stream;

/**
 * Template browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class BrowsersTemplate {

    List<Browser> browsers;

    public Stream<Browser> getStream() {
        return browsers.stream();
    }

    public class Browser {
        String type;
        String version;

        public String getType() {
            return type;
        }

        public String getVersion() {
            return version;
        }

        public BrowserType toBrowserType() {
            return valueOf(getType().replace("docker-", "").toUpperCase());

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
