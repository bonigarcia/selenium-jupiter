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

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.EdgeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import io.github.bonigarcia.wdm.InternetExplorerDriverManager;
import io.github.bonigarcia.wdm.OperaDriverManager;
import io.github.bonigarcia.wdm.PhantomJsDriverManager;

/**
 * Selenium extension for Jupiter (JUnit 5) tests.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class SeleniumExtension
        implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private boolean chrome, firefox, opera, iexplorer, edge, phantomjs;

    private List<WebDriver> webDriverList = new ArrayList<>();

    @Override
    public boolean supports(ParameterContext parameterContext,
            ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Parameter parameter = parameterContext.getParameter();
        return WebDriver.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolve(ParameterContext parameterContext,
            ExtensionContext extensionContext)
            throws ParameterResolutionException {

        Parameter parameter = parameterContext.getParameter();
        Class<?> type = parameter.getType();
        WebDriver webDriver = null;

        if (type == ChromeDriver.class) {
            if (!chrome) {
                chrome = true;
                ChromeDriverManager.getInstance().setup();
            }
            webDriver = new ChromeDriver();
        } else if (type == FirefoxDriver.class) {
            if (!firefox) {
                FirefoxDriverManager.getInstance().setup();
            }
            webDriver = new FirefoxDriver();
        } else if (type == OperaDriver.class) {
            if (!opera) {
                OperaDriverManager.getInstance().setup();
            }
            webDriver = new OperaDriver();
        } else if (type == InternetExplorerDriver.class) {
            if (!iexplorer) {
                InternetExplorerDriverManager.getInstance().setup();
            }
            webDriver = new InternetExplorerDriver();
        } else if (type == EdgeDriver.class) {
            if (!edge) {
                EdgeDriverManager.getInstance().setup();
            }
            webDriver = new EdgeDriver();
        } else if (type == PhantomJSDriver.class) {
            if (!phantomjs) {
                PhantomJsDriverManager.getInstance().setup();
            }
            webDriver = new PhantomJSDriver();
        }

        if (webDriver != null) {
            webDriverList.add(webDriver);
        }

        return webDriver;
    }

    @Override
    public void beforeEach(TestExtensionContext context) throws Exception {
    }

    @Override
    public void afterEach(TestExtensionContext context) throws Exception {
        for (WebDriver w : webDriverList) {
            if (w != null) {
                w.close();
            }
        }
        webDriverList.clear();
    }

}
