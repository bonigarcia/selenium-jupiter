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
package io.github.bonigarcia.test.annotations;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Parameter;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import io.appium.java_client.AppiumDriver;
import io.github.bonigarcia.SeleniumJupiterException;
import io.github.bonigarcia.handler.AppiumDriverHandler;
import io.github.bonigarcia.handler.ChromeDriverHandler;
import io.github.bonigarcia.handler.DriverHandler;
import io.github.bonigarcia.handler.EdgeDriverHandler;
import io.github.bonigarcia.handler.FirefoxDriverHandler;
import io.github.bonigarcia.handler.OperaDriverHandler;
import io.github.bonigarcia.handler.RemoteDriverHandler;
import io.github.bonigarcia.handler.SafariDriverHandler;
import io.github.bonigarcia.test.forced.ForcedAppiumJupiterTest;
import io.github.bonigarcia.test.forced.ForcedBadChromeJupiterTest;
import io.github.bonigarcia.test.forced.ForcedBadFirefoxJupiterTest;
import io.github.bonigarcia.test.forced.ForcedBadRemoteJupiterTest;
import io.github.bonigarcia.test.forced.ForcedEdgeJupiterTest;
import io.github.bonigarcia.test.forced.ForcedOperaJupiterTest;
import io.github.bonigarcia.test.forced.ForcedSafariJupiterTest;

public class ForcedAnnotationReaderTest {

    static Stream<Arguments> forcedTestProvider() {
        return Stream.of(
                Arguments.of(AppiumDriverHandler.class,
                        ForcedAppiumJupiterTest.class, AppiumDriver.class,
                        "appiumNoCapabilitiesTest"),
                Arguments.of(AppiumDriverHandler.class,
                        ForcedAppiumJupiterTest.class, AppiumDriver.class,
                        "appiumWithCapabilitiesTest"),
                Arguments.of(ChromeDriverHandler.class,
                        ForcedBadChromeJupiterTest.class, ChromeDriver.class,
                        "chromeTest"),
                Arguments.of(FirefoxDriverHandler.class,
                        ForcedBadFirefoxJupiterTest.class, FirefoxDriver.class,
                        "firefoxTest"),
                Arguments.of(RemoteDriverHandler.class,
                        ForcedBadRemoteJupiterTest.class, RemoteWebDriver.class,
                        "remoteTest"),
                Arguments.of(EdgeDriverHandler.class,
                        ForcedEdgeJupiterTest.class, EdgeDriver.class,
                        "edgeTest"),
                Arguments.of(OperaDriverHandler.class,
                        ForcedOperaJupiterTest.class, OperaDriver.class,
                        "operaTest"),
                Arguments.of(SafariDriverHandler.class,
                        ForcedSafariJupiterTest.class, SafariDriver.class,
                        "safariTest"));
    }

    @ParameterizedTest
    @MethodSource("forcedTestProvider")
    void forcedTest(Class<? extends DriverHandler> handler, Class<?> testClass,
            Class<?> driverClass, String testName) throws Exception {
        Parameter parameter = testClass.getMethod(testName, driverClass)
                .getParameters()[0];
        assertThrows(SeleniumJupiterException.class, () -> {
            handler.getDeclaredConstructor(Parameter.class,
                    ExtensionContext.class).newInstance(parameter, null)
                    .resolve();
        });
    }

}
