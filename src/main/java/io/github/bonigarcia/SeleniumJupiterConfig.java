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

import static com.typesafe.config.ConfigFactory.load;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;

import java.net.MalformedURLException;
import java.net.URL;

import com.typesafe.config.Config;

/**
 * Configuration (wrapper for Java properties).
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.1.3
 */
public class SeleniumJupiterConfig {

    private static SeleniumJupiterConfig instance;
    private Config config;

    protected SeleniumJupiterConfig() {
        config = load(SeleniumJupiterConfig.class.getClassLoader(), getProperty(
                "sel.jup.properties", "selenium-jupiter.properties"));
    }

    public static synchronized SeleniumJupiterConfig getConfig() {
        if (instance == null) {
            instance = new SeleniumJupiterConfig();
        }
        return instance;
    }

    public String getString(String key) {
        String value = "";
        if (!key.equals("")) {
            // dots are not allowed in POSIX environmental variables
            value = getenv(key.replace(".", "_"));
            if (value == null) {
                value = config.getString(key);
            }
        }
        return value;
    }

    public int getInt(String key) {
        return config.getInt(key);
    }

    public boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    public URL getUrl(String key) throws MalformedURLException {
        return new URL(config.getString(key));
    }

}
