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

import java.util.List;

/**
 * JSON parser for browsers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.0.0
 */
public class BrowserParser {

    List<BrowserEntry> browsers;

    @Override
    public String toString() {
        return "Browsers [browsers=" + browsers + "]";
    }

    class BrowserEntry {
        String type;
        String version;

        @Override
        public String toString() {
            return "BrowserEntry [type=" + type + ", version=" + version + "]";
        }

    }

}
