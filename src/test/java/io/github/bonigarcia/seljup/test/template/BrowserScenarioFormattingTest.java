/*
 * (C) Copyright 2025 Boni Garcia (https://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup.test.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

import com.google.gson.internal.LinkedTreeMap;

import io.github.bonigarcia.seljup.BrowserBuilder;
import io.github.bonigarcia.seljup.BrowserScenarioTest;
import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;

class BrowserScenarioFormattingTest {

    @Test
    void replaceAllPlaceholders() {
        LinkedTreeMap<String, String> capabilities = new LinkedTreeMap<>();
        capabilities.put("custom:cap-1", "value-1");
        capabilities.put("custom:cap-2", "custom-value-2");
        capabilities.put("custom:cap-3", "true");
        Browser browser = new BrowserBuilder("chrome-in-docker")
                .version("latest")
                .arguments(new String[] { "--disable-gpu", "--no-sandbox" })
                .preferences(new String[] {
                        "media.navigator.permission.disabled=true",
                        "media.navigator.streams.fake=true" })
                .capabilities(capabilities).remoteUrl("http://localhost:4444/")
                .build();
        String result = BrowserScenarioTest.NameFormatter.format(
                "{displayName}, {type}, {version}, {arguments}, {preferences}, {capabilities}, {remoteUrl}",
                "Sample Test", browser);
        assertEquals(
                """
                        Sample Test, chrome-in-docker, latest, [--disable-gpu, --no-sandbox], \
                        [media.navigator.permission.disabled=true, media.navigator.streams.fake=true], \
                        {custom:cap-1=value-1, custom:cap-2=custom-value-2, custom:cap-3=true}, http://localhost:4444/""",
                result);
    }

    @Test
    void skipNullBrowserAttributes() {
        Browser browser = new BrowserBuilder(null).version(null).arguments(null)
                .preferences(null).capabilities(null).remoteUrl(null).build();
        String result = BrowserScenarioTest.NameFormatter.format(
                "{displayName}, {type}, {version}, {arguments}, {preferences}, {capabilities}, {remoteUrl}",
                "Sample Test", browser);
        assertEquals(
                "Sample Test, {type}, {version}, {arguments}, {preferences}, {capabilities}, {remoteUrl}",
                result);
    }

    @Test
    void throwExceptionWhenNamePatternIsNull() {
        Browser browser = new BrowserBuilder("chrome").build();
        assertThrows(PreconditionViolationException.class,
                () -> BrowserScenarioTest.NameFormatter.format(null,
                        "Sample Test", browser));
    }

    @Test
    void throwExceptionWhenDisplayNameIsNull() {
        Browser browser = new BrowserBuilder("chrome").build();
        assertThrows(PreconditionViolationException.class,
                () -> BrowserScenarioTest.NameFormatter.format(
                        "{displayName} - {type} {version}", null, browser));
    }

    @Test
    void throwExceptionWhenBrowserIsNull() {
        assertThrows(PreconditionViolationException.class,
                () -> BrowserScenarioTest.NameFormatter.format(
                        "{displayName} - {type} {version}", "Sample Test",
                        null));
    }

    @Test
    void throwExceptionWhenNamePatternIsEmpty() {
        Browser browser = new BrowserBuilder("chrome").build();
        assertThrows(PreconditionViolationException.class,
                () -> BrowserScenarioTest.NameFormatter.format("   ",
                        "Sample Test", browser));
    }

}
