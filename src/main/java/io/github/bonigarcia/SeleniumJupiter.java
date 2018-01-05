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
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import io.github.bonigarcia.wdm.WdmConfig;
import io.github.bonigarcia.wdm.WebDriverManagerException;

/**
 * Collection of utility features.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumJupiter {

    static final Logger log = getLogger(lookup().lookupClass());

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

    public static String getOutputFolder(ExtensionContext context) {
        String outputFolder = getString("sel.jup.output.folder");
        Optional<Class<?>> testInstance = context.getTestClass();
        if (outputFolder.equalsIgnoreCase("surefire-reports")
                && testInstance.isPresent()) {
            outputFolder = "./target/surefire-reports/"
                    + testInstance.get().getName();
        } else {
            outputFolder = ".";
        }
        log.debug("Output folder {}", outputFolder);

        File outputFolderFile = new File(outputFolder);
        if (!outputFolderFile.exists()) {
            outputFolderFile.mkdirs();
        }
        return outputFolder;
    }

    private static String getProperty(String key) {
        String value = null;
        Properties properties = new Properties();
        try {
            InputStream inputStream = WdmConfig.class.getResourceAsStream(
                    System.getProperty("sel.jup.properties",
                            "/selenium-jupiter.properties"));
            properties.load(inputStream);
            value = properties.getProperty(key);
        } catch (Exception e) {
            throw new WebDriverManagerException(e);
        } finally {
            if (value == null) {
                log.trace("Property key {} not found, using default value",
                        key);
                value = "";
            }
        }
        return value;
    }

}
