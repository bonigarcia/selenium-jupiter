[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.bonigarcia/selenium-jupiter/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.bonigarcia/selenium-jupiter)
[![Build Status](https://travis-ci.org/bonigarcia/selenium-jupiter.svg?branch=master)](https://travis-ci.org/bonigarcia/selenium-jupiter)
[![Quality Gate](https://sonarcloud.io/api/badges/gate?key=io.github.bonigarcia:selenium-jupiter)](https://sonarcloud.io/dashboard/index/io.github.bonigarcia:selenium-jupiter)
[![codecov](https://codecov.io/gh/bonigarcia/selenium-jupiter/branch/master/graph/badge.svg)](https://codecov.io/gh/bonigarcia/selenium-jupiter)
[![badge-jdk](https://img.shields.io/badge/jdk-8-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![License badge](https://img.shields.io/badge/license-Apache2-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Support badge]( https://img.shields.io/badge/support-sof-green.svg)](http://stackoverflow.com/questions/tagged/selenium-jupiter)
[![Twitter](https://img.shields.io/badge/follow-@boni_gg-green.svg)](https://twitter.com/boni_gg)

# selenium-jupiter [![][Logo]][GitHub Repository]

*selenium-jupiter* is a JUnit 5 extension aimed to ease the use of Selenium WebDriver in Jupiter tests. This library is open source, released under the terms of [Apache 2.0 License].

## Basic usage

In order to include *selenium-jupiter* in a Maven project, first add the following dependency to your `pom.xml` (Java 8 required):

```xml
<dependency>
	<groupId>io.github.bonigarcia</groupId>
	<artifactId>selenium-jupiter</artifactId>
	<version>2.0.0</version>
</dependency>
```

*selenium-jupiter* will be tipically used by tests. In that case, the scope of the dependency should be test (`<scope>test</scope>`).

Once we have included this dependency, *selenium-jupiter* will manage the WebDriver instances and inject them as parameters in your JUnit 5 tests:

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class SeleniumJupiterTest {

    @Test
    public void testChrome(ChromeDriver driver) {
    	// use chrome in this test
    }

    @Test
    public void testFirefox(FirefoxDriver driver) {
    	// use firefox in this test
    }

}
```

Internally, *selenium-jupiter* uses [WebDriverManager] to manage the WebDriver binaries (i.e. *chromedriver*, *geckodriver*,  *operadriver*, and so on) required to use local browsers.

## Docker browsers

As of version 2, *selenium-jupiter* allows to use browsers in [Docker] containers. The only requirement is to get installed [Docker Engine] in the machine running the tests. A simple example using this feature is the following:

```java
import static io.github.bonigarcia.BrowserType.CHROME;
import static io.github.bonigarcia.BrowserType.FIREFOX;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class SeleniumJupiterDockerTest {

    @Test
    public void testChrome(
            @DockerBrowser(type = CHROME, version = "latest") RemoteWebDriver driver) {
        // use chrome (latest version) in this test
    }

    @Test
    public void testFirefox(
            @DockerBrowser(type = FIREFOX, version = "57.0") RemoteWebDriver driver) {
        // use firefox (version 57.0) in this test
    }

}
```

## Documentation

You can find more details and examples on the [selenium-jupiter user guide].

## About

selenium-jupiter (Copyright &copy; 2018) is a project by [Boni Garcia] licensed under [Apache 2.0 License]. Comments, questions and suggestions are always very [welcome][selenium-jupiter issues]!

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[Boni Garcia]: http://bonigarcia.github.io/
[Docker]: https://www.docker.com/
[Docker Engine]: https://www.docker.com/get-docker
[GitHub Repository]: https://github.com/bonigarcia/selenium-jupiter
[Logo]: http://bonigarcia.github.io/img/selenium-jupiter.png
[selenium-jupiter user guide]: https://bonigarcia.github.io/selenium-jupiter/
[selenium-jupiter issues]: https://github.com/bonigarcia/selenium-jupiter/issues
[Selenium Webdriver]: http://docs.seleniumhq.org/projects/webdriver/
[WebDriverManager]: https://github.com/bonigarcia/webdrivermanager
