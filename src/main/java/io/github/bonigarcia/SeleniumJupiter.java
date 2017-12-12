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

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import io.github.bonigarcia.wdm.WdmConfig;
import io.github.bonigarcia.wdm.WebDriverManagerException;

/**
 * Collection of utility features.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumJupiter {

    // Chrome, Firefox, Opera
    public static final String ARGS = "args";
    public static final String BINARY = "binary";

    // Chrome, Firefox, Opera
    public static final String EXTENSION = "extension";

    // Edge
    public static final String PAGE_LOAD_STRATEGY = "pageLoadStrategy";

    // Safari
    public static final String USE_CLEAN_SESSION = "useCleanSession";
    public static final String USE_TECHNOLOGY_PREVIEW = "useTechnologyPreview";

    SeleniumJupiter() {
        throw new IllegalStateException("Utility class");
    }

    public static String getString(String key) {
        String value = "";
        if (!key.equals("")) {
            value = System.getenv(key.toUpperCase().replace(".", "_"));
            if (value == null) {
                value = System.getProperty(key);
            }
            if (value == null) {
                value = getProperty(key);
            }
        }
        return value;
    }

    public static int getInt(String key) {
        return parseInt(getString(key));
    }

    public static boolean getBoolean(String key) {
        return parseBoolean(getString(key));
    }

    public static URL getUrl(String key) {
        try {
            return new URL(getString(key));
        } catch (MalformedURLException e) {
            throw new WebDriverManagerException(e);
        }
    }

    private static String getProperty(String key) {
        Properties properties = new Properties();
        try {
            InputStream inputStream = WdmConfig.class.getResourceAsStream(
                    System.getProperty("sel.jup.properties",
                            "/selenium-jupiter.properties"));
            properties.load(inputStream);
        } catch (Exception e) {
            throw new WebDriverManagerException(e);
        }
        return properties.getProperty(key);
    }

}
