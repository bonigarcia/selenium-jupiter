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
package io.github.bonigarcia.seljup.config;

import static io.github.bonigarcia.seljup.OutputHandler.BASE64_AND_PNG_KEY;
import static io.github.bonigarcia.seljup.OutputHandler.BASE64_KEY;
import static io.github.bonigarcia.seljup.OutputHandler.PNG_KEY;
import static io.github.bonigarcia.seljup.OutputHandler.SUREFIRE_REPORTS_KEY;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import org.slf4j.Logger;

import io.github.bonigarcia.seljup.SeleniumJupiterException;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Configuration class.
 *
 * @author Boni Garcia
 * @since 2.1.0
 */
public class Config {

    final Logger log = getLogger(lookup().lookupClass());

    WebDriverManager manager;

    ConfigKey<String> properties = new ConfigKey<>("sel.jup.properties",
            String.class, "selenium-jupiter.properties");
    ConfigKey<String> outputFolder = new ConfigKey<>("sel.jup.output.folder",
            String.class);

    ConfigKey<String> seleniumServerUrl = new ConfigKey<>(
            "sel.jup.selenium.server.url", String.class);

    ConfigKey<Boolean> vnc = new ConfigKey<>("sel.jup.vnc", Boolean.class);

    ConfigKey<Boolean> recording = new ConfigKey<>("sel.jup.recording",
            Boolean.class);
    ConfigKey<Boolean> recordingWhenFailure = new ConfigKey<>(
            "sel.jup.recording.when.failure", Boolean.class);

    ConfigKey<Boolean> screenshot = new ConfigKey<>("sel.jup.screenshot",
            Boolean.class);
    ConfigKey<Boolean> screenshotWhenFailure = new ConfigKey<>(
            "sel.jup.screenshot.when.failure", Boolean.class);
    ConfigKey<String> screenshotFormat = new ConfigKey<>(
            "sel.jup.screenshot.format", String.class);

    ConfigKey<String> browserTemplateJsonFile = new ConfigKey<>(
            "sel.jup.browser.template.json.file", String.class);
    ConfigKey<String> browserTemplateJsonContent = new ConfigKey<>(
            "sel.jup.browser.template.json.content", String.class);

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

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    // Getters and setters

    public String getProperties() {
        return resolve(properties);
    }

    public void setProperties(String properties) {
        this.properties.setValue(properties);
    }

    public String getSeleniumServerUrl() {
        String url = resolve(seleniumServerUrl);
        if (isNullOrEmpty(url)) {
            url = System.getProperty("webdriver.remote.server");
        }
        return url;
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

    public boolean isVnc() {
        return resolve(vnc);
    }

    public void setVnc(boolean value) {
        this.vnc.setValue(value);
    }

    public boolean isRecording() {
        return resolve(recording);
    }

    public void setRecording(boolean value) {
        this.recording.setValue(value);
    }

    public boolean isRecordingWhenFailure() {
        return resolve(recordingWhenFailure);
    }

    public void setRecordingWhenFailure(boolean value) {
        this.recordingWhenFailure.setValue(value);
    }

    public boolean isScreenshot() {
        return resolve(screenshot);
    }

    public void setScreenshot(boolean value) {
        this.screenshot.setValue(value);
    }

    public boolean isScreenshotWhenFailure() {
        return resolve(screenshotWhenFailure);
    }

    public void setScreenshotWhenFailure(boolean value) {
        this.screenshotWhenFailure.setValue(value);
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

    public WebDriverManager getManager() {
        return manager;
    }

    public void setManager(WebDriverManager manager) {
        this.manager = manager;
    }

    // Readable API methods

    public void enableVnc() {
        setVnc(true);
    }

    public void enableScreenshot() {
        setScreenshot(true);
    }

    public void enableRecording() {
        setRecording(true);
    }

    public void enableRecordingWhenFailure() {
        setRecordingWhenFailure(true);
    }

    public void enableScreenshotWhenFailure() {
        setScreenshotWhenFailure(true);
    }

    public void useSurefireOutputFolder() {
        setOutputFolder(SUREFIRE_REPORTS_KEY);
    }

    public void takeScreenshotAsBase64() {
        setScreenshotFormat(BASE64_KEY);
    }

    public void takeScreenshotAsPng() {
        setScreenshotFormat(PNG_KEY);
    }

    public void takeScreenshotAsBase64AndPng() {
        setScreenshotFormat(BASE64_AND_PNG_KEY);
    }

}
