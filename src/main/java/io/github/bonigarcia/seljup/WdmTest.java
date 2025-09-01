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
package io.github.bonigarcia.seljup;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionContext;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Class to store the WebDriverManager instances and the test reporter.
 *
 * @author Boni Garcia
 * @since 6.3.0
 */
public class WdmTest {

    static final String FORMATTED_INFO = "<pre>%s:" + System.lineSeparator()
            + "%s</pre>";

    List<WebDriverManager> wdmList;
    ExtentTest test;
    ExtensionContext extensionContext;

    public WdmTest(ExtentReports report, ExtensionContext extensionContext) {
        this.wdmList = new CopyOnWriteArrayList<>();
        this.extensionContext = extensionContext;
        this.test = createExtentTest(report, extensionContext);
    }

    public List<WebDriverManager> getWdmList() {
        return wdmList;
    }

    public ExtentTest getTest() {
        return test;
    }

    private ExtentTest createExtentTest(ExtentReports report,
            ExtensionContext context) {
        String displayName = context.getDisplayName();
        String testName = context.getTestClass()
                .map(testClass -> testClass.getSimpleName() + "." + displayName)
                .orElse(displayName);
        ExtentTest extentTest = report.createTest(testName);
        context.getTags().forEach(extentTest::assignCategory);
        return extentTest;
    }

    public void gatherBrowserData() {
        getWdmList().forEach(wdm -> {
            wdm.getWebDriverList().forEach(driver -> {
                String driverInfo = driver.toString();

                // Screenshot
                String base64Screenshot = ScreenshotManager
                        .getBase64Screenshot(driver);
                test.addScreenCaptureFromBase64String(base64Screenshot,
                        driverInfo);

                // Logs
                List<Map<String, Object>> logs = wdm.getLogs(driver);
                if (logs != null && !logs.isEmpty()) {
                    String logAsString = logs.stream()
                            .map(entry -> String.format("[%s] [%s] %s",
                                    entry.get("datetime"), entry.get("type"),
                                    entry.get("message")))
                            .collect(
                                    Collectors.joining(System.lineSeparator()));
                    test.info(String.format(FORMATTED_INFO,
                            driverInfo + " console", logAsString));
                }
            });
        });
    }

}
