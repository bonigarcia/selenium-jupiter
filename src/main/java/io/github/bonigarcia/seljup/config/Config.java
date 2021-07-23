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
package io.github.bonigarcia.seljup.config;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import org.slf4j.Logger;

import io.github.bonigarcia.seljup.SeleniumJupiterException;

/**
 * Configuration class.
 *
 * @author Boni Garcia
 * @since 2.1.0
 */
public class Config {

    final Logger log = getLogger(lookup().lookupClass());

    static final String HOME = "~";

    ConfigKey<String> properties = new ConfigKey<>("sel.jup.properties",
            String.class, "selenium-jupiter.properties");
    ConfigKey<String> seleniumServerUrl = new ConfigKey<>(
            "sel.jup.selenium.server.url", String.class);
    ConfigKey<String> outputFolder = new ConfigKey<>("sel.jup.output.folder",
            String.class);
    ConfigKey<Boolean> recordingWhenFailure = new ConfigKey<>(
            "sel.jup.recording.when.failure", Boolean.class);
    ConfigKey<String> screenshotAtTheEndOfTests = new ConfigKey<>(
            "sel.jup.screenshot.at.the.end.of.tests", String.class);
    ConfigKey<String> screenshotFormat = new ConfigKey<>(
            "sel.jup.screenshot.format", String.class);
    ConfigKey<String> browserTemplateJsonFile = new ConfigKey<>(
            "sel.jup.browser.template.json.file", String.class);
    ConfigKey<String> browserTemplateJsonContent = new ConfigKey<>(
            "sel.jup.browser.template.json.content", String.class);

    ConfigKey<Integer> serverPort = new ConfigKey<>("sel.jup.server.port",
            Integer.class);
    ConfigKey<String> serverPath = new ConfigKey<>("sel.jup.server.path",
            String.class);
    ConfigKey<Integer> serverTimeoutSec = new ConfigKey<>(
            "sel.jup.server.timeout.sec", Integer.class);

    private <T> T resolve(ConfigKey<T> configKey) {
        String strValue = null;
        String name = configKey.getName();
        T tValue = configKey.getValue();
        Class<T> type = configKey.getType();

        strValue = System.getenv(name.toUpperCase().replace(".", "_"));
        if (strValue == null) {
            strValue = System.getProperty(name);
        }
        if (strValue == null && tValue != null) {
            return tValue;
        }
        if (strValue == null) {
            strValue = getProperty(name);
        }
        return parse(type, strValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T parse(Class<T> type, String strValue) {
        T output = null;
        if (type.equals(String.class)) {
            output = (T) strValue;
        } else if (type.equals(Integer.class)) {
            output = (T) Integer.valueOf(strValue);
        } else if (type.equals(Boolean.class)) {
            output = (T) Boolean.valueOf(strValue);
        } else {
            throw new SeleniumJupiterException(
                    "Type " + type.getTypeName() + " cannot be parsed");
        }
        return output;
    }

    private String getProperty(String key) {
        String value = null;
        Properties props = new Properties();
        try {
            InputStream inputStream = Config.class
                    .getResourceAsStream("/" + getProperties());
            props.load(inputStream);
            value = props.getProperty(key);
        } catch (Exception e) {
            throw new SeleniumJupiterException(e);
        } finally {
            if (value == null) {
                log.trace("Property key {} not found, using default value",
                        key);
                value = "";
            }
        }
        return value;
    }

    public void reset() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType() == ConfigKey.class) {
                try {
                    ((ConfigKey<?>) field.get(this)).reset();
                } catch (Exception e) {
                    log.warn("Exception reseting {}", field);
                }
            }
        }
    }

    // Getters and setters

    public String getProperties() {
        return resolve(properties);
    }

    public void setProperties(String properties) {
        this.properties.setValue(properties);
    }

    public String getSeleniumServerUrl() {
        return resolve(seleniumServerUrl);
    }

    public void setSeleniumServerUrl(String value) {
        this.seleniumServerUrl.setValue(value);
    }

    public String getOutputFolder() {
        return resolve(outputFolder);
    }

    public void setOutputFolder(String value) {
        this.outputFolder.setValue(value);
    }

    public boolean isRecordingWhenFailure() {
        return resolve(recordingWhenFailure);
    }

    public void setRecordingWhenFailure(boolean value) {
        this.recordingWhenFailure.setValue(value);
    }

    public String getScreenshotAtTheEndOfTests() {
        return resolve(screenshotAtTheEndOfTests);
    }

    public void setScreenshotAtTheEndOfTests(String value) {
        this.screenshotAtTheEndOfTests.setValue(value);
    }

    public String getScreenshotFormat() {
        return resolve(screenshotFormat);
    }

    public void setScreenshotFormat(String value) {
        this.screenshotFormat.setValue(value);
    }

    public String getBrowserTemplateJsonFile() {
        return resolve(browserTemplateJsonFile);
    }

    public void setBrowserTemplateJsonFile(String value) {
        this.browserTemplateJsonFile.setValue(value);
    }

    public String getBrowserTemplateJsonContent() {
        return resolve(browserTemplateJsonContent);
    }

    public void setBrowserTemplateJsonContent(String value) {
        this.browserTemplateJsonContent.setValue(value);
    }

    public int getServerPort() {
        return resolve(serverPort);
    }

    public Config setServerPort(int value) {
        this.serverPort.setValue(value);
        return this;
    }

    public String getServerPath() {
        return resolve(serverPath);
    }

    public Config setServerPath(String value) {
        this.serverPath.setValue(value);
        return this;
    }

    public int getServerTimeoutSec() {
        return resolve(serverTimeoutSec);
    }

    public Config setServerTimeoutSec(int value) {
        this.serverTimeoutSec.setValue(value);
        return this;
    }

    // Custom values

    public void useSurefireOutputFolder() {
        this.outputFolder.setValue("surefire-reports");
    }

    public void enableScreenshotAtTheEndOfTests() {
        this.screenshotAtTheEndOfTests.setValue("true");
    }

    public void disableScreenshotAtTheEndOfTests() {
        this.screenshotAtTheEndOfTests.setValue("false");
    }

    public void takeScreenshotAsBase64() {
        this.screenshotFormat.setValue("base64");
    }

    public void takeScreenshotAsPng() {
        this.screenshotFormat.setValue("png");
    }

    public void takeScreenshotAsBase64AndPng() {
        this.screenshotFormat.setValue("base64andpng");
    }

}
