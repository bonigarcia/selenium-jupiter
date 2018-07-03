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
package io.github.bonigarcia.test.template;

import static io.github.bonigarcia.BrowserBuilder.android;
import static io.github.bonigarcia.BrowserBuilder.appium;
import static io.github.bonigarcia.BrowserBuilder.chrome;
import static io.github.bonigarcia.BrowserBuilder.chromeInDocker;
import static io.github.bonigarcia.BrowserBuilder.edge;
import static io.github.bonigarcia.BrowserBuilder.firefox;
import static io.github.bonigarcia.BrowserBuilder.firefoxInDocker;
import static io.github.bonigarcia.BrowserBuilder.iexplorer;
import static io.github.bonigarcia.BrowserBuilder.opera;
import static io.github.bonigarcia.BrowserBuilder.operaInDocker;
import static io.github.bonigarcia.BrowserBuilder.phantomjs;
import static io.github.bonigarcia.BrowserBuilder.safari;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.bonigarcia.BrowserBuilder;
import io.github.bonigarcia.BrowsersTemplate.Browser;

public class BrowserBuilderTest {

    static Stream<BrowserBuilder> browserBuilderProvider() {
        return Stream.of(chrome(), firefox(), opera(), android(), appium(),
                edge(), chromeInDocker(), firefoxInDocker(), operaInDocker(),
                phantomjs(), safari(), iexplorer());
    }

    @ParameterizedTest
    @MethodSource("browserBuilderProvider")
    void templateTest(BrowserBuilder browserBuilder) {
        Browser browser = browserBuilder.version("").browserName("")
                .deviceName("").build();
        assertThat(browser, notNullValue());
    }

}
