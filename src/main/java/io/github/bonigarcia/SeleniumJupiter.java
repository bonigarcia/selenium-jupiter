/*
 * (C) Copyright 2017 Boni Garcia (http://bonigarcia.github.io/)
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

/**
 * Collection of options/capabilities names.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumJupiter {

    // Chrome, Firefox, Opera
    public static final String ARGS = "args";
    public static final String BINARY = "binary";

    // Chrome, Opera
    public static final String EXTENSIONS = "extensions";
    public static final String EXTENSION_FILES = "extensionFiles";

    // Edge
    public static final String PAGE_LOAD_STRATEGY = "pageLoadStrategy";

    // Safari
    public static final String PORT = "port";
    public static final String USE_CLEAN_SESSION = "useCleanSession";
    public static final String USE_TECHNOLOGY_PREVIEW = "useTechnologyPreview";

}
