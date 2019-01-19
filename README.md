[![Maven Central](https://img.shields.io/maven-central/v/io.github.bonigarcia/selenium-jupiter.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3Aio.github.bonigarcia%20a%3Aselenium-jupiter)
[![Build Status](https://travis-ci.org/bonigarcia/selenium-jupiter.svg?branch=master)](https://travis-ci.org/bonigarcia/selenium-jupiter)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=io.github.bonigarcia:selenium-jupiter&metric=alert_status)](https://sonarcloud.io/dashboard/index/io.github.bonigarcia:selenium-jupiter)
[![codecov](https://codecov.io/gh/bonigarcia/selenium-jupiter/branch/master/graph/badge.svg)](https://codecov.io/gh/bonigarcia/selenium-jupiter)
[![badge-jdk](https://img.shields.io/badge/jdk-8-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![License badge](https://img.shields.io/badge/license-Apache2-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Support badge]( https://img.shields.io/badge/support-sof-green.svg)](http://stackoverflow.com/questions/tagged/selenium-jupiter)
[![Twitter](https://img.shields.io/badge/follow-@boni_gg-green.svg)](https://twitter.com/boni_gg)

# Selenium-Jupiter [![][Logo]][GitHub Repository]

*Selenium-Jupiter* is a JUnit 5 extension aimed to ease the use of Selenium (WebDriver and Grid) in JUnit 5 tests. This library is open source, released under the terms of [Apache 2.0 License].

## Basic usage

In order to include *Selenium-Jupiter* in a Maven project, first add the following dependency to your `pom.xml` (Java 8 required):

```xml
<dependency>
	<groupId>io.github.bonigarcia</groupId>
	<artifactId>selenium-jupiter</artifactId>
	<version>3.0.0</version>
</dependency>
```

*Selenium-Jupiter* is typically used by tests. In that case, the scope of the dependency should be test (`<scope>test</scope>`).

Once we have included this dependency, *Selenium-Jupiter* manages the WebDriver instances and inject them as parameters in your JUnit 5 tests:

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.seljup.SeleniumExtension;

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

Internally, *Selenium-Jupiter* uses [WebDriverManager] to manage the WebDriver binaries (i.e. *chromedriver*, *geckodriver*,  *operadriver*, and so on) required to use local browsers.

## Docker browsers

As of version 2, *Selenium-Jupiter* allows to use browsers in [Docker] containers. The only requirement is to get installed [Docker Engine] in the machine running the tests. A simple example using this feature is the following:

```java
import static io.github.bonigarcia.seljup.BrowserType.ANDROID;
import static io.github.bonigarcia.seljup.BrowserType.CHROME;
import static io.github.bonigarcia.seljup.BrowserType.FIREFOX;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class SeleniumJupiterDockerTest {

    @Test
    public void testChrome(
            @DockerBrowser(type = CHROME, version = "latest") RemoteWebDriver driver) {
        // use Chrome (latest version) in this test
    }

    @Test
    public void testFirefox(
            @DockerBrowser(type = FIREFOX, version = "64.0") RemoteWebDriver driver) {
        // use Firefox (version 64.0) in this test
    }

    @Test
    public void testAndroid(
            @DockerBrowser(type = ANDROID, version = "8.1") RemoteWebDriver driver) {
        // use Android (version 8.1) in this test
    }

}
```


## Selenium-Jupiter CLI

As of version 2.2.0, Selenium-Jupiter can used interactively from the Command Line Interface (CLI), i.e. the shell, to get VNC sessions of Docker browsers (Chrome, Firefox, Opera, Android). There are two ways of using this feature:

* Directly from the source code, using Maven. The command to be used is ``mvn exec:java -Dexec.args="browserName"``. For instance:

```
> mvn exec:java -Dexec.args="chrome"
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building Selenium-Jupiter 3.0.0
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.6.0:java (default-cli) @ selenium-jupiter ---
[INFO] Using SeleniumJupiter to execute chrome (latest) in Docker
[INFO] Using CHROME version 71.0 (latest)
[INFO] Starting Docker container aerokube/selenoid:1.8.4
[DEBUG] Creating WebDriver for CHROME at http://172.17.0.1:32782/wd/hub
Jan 07, 2019 6:54:19 PM org.openqa.selenium.remote.ProtocolHandshake createSession
INFO: Detected dialect: OSS
[INFO] Starting Docker container psharkey/novnc:3.3-t6
[INFO] Session id fe492bee1ecebceb645cf58275a63bd6
[INFO] VNC URL (copy and paste in a browser navigation bar to interact with remote session)
[INFO] http://172.17.0.1:32783/vnc.html?host=172.17.0.1&port=32782&path=vnc/fe492bee1ecebceb645cf58275a63bd6&resize=scale&autoconnect=true&password=selenoid
[INFO] Press ENTER to exit

[INFO] Stopping Docker container aerokube/selenoid:1.8.4
[INFO] Stopping Docker container psharkey/novnc:3.3-t6
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 21.240 s
[INFO] Finished at: 2019-01-07T16:26:47+01:00
[INFO] Final Memory: 33M/453M
[INFO] ------------------------------------------------------------------------
```

* Using Selenium-Jupiter as a *fat-jar* (i.e. Selenium-Jupiter with all its dependencies in a single executable JAR file). This JAR file can downloaded from [here](https://github.com/bonigarcia/selenium-jupiter/releases/download/selenium-jupiter-3.0.0/selenium-jupiter-3.0.0-fat.jar) and also it can be created using the command ``mvn compile assembly:single`` from the source code. Once you get the *fat-jar*, you simply need to use the command ``java -jar selenium-jupiter-3.0.0-fat.jar browserName``, for instance:

```
> java -jar selenium-jupiter-3.0.0-fat.jar chrome
[INFO] Using SeleniumJupiter to execute chrome (latest) in Docker
[INFO] Using CHROME version 71.0 (latest)
[INFO] Starting Docker container aerokube/selenoid:1.8.4
[DEBUG] Creating WebDriver for CHROME at http://172.17.0.1:32784/wd/hub
Jan 07, 2019 6:55:17 PM org.openqa.selenium.remote.ProtocolHandshake createSession
INFO: Detected dialect: OSS
[INFO] Starting Docker container psharkey/novnc:3.3-t6
[INFO] Session id 8edd28c130bb2bc62f8e4467c20f4dc0
[INFO] VNC URL (copy and paste in a browser navigation bar to interact with remote session)
[INFO] http://172.17.0.1:32785/vnc.html?host=172.17.0.1&port=32784&path=vnc/8edd28c130bb2bc62f8e4467c20f4dc0&resize=scale&autoconnect=true&password=selenoid
[INFO] Press ENTER to exit

[INFO] Stopping Docker container aerokube/selenoid:1.8.4
[INFO] Stopping Docker container psharkey/novnc:3.3-t6
```

## Selenium-Jupiter Server

As of version 3.0.0, Selenium-Jupiter can used as a server. To start this mode, the shell is used. Once again, two options are allowed:

* Directly from the source code and Maven. The command to be used is ``mvn exec:java -Dexec.args="server <port>"``. If the second argument is not specified, the default port will be used (4042):

```
$ mvn exec:java -Dexec.args="server"
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building Selenium-Jupiter 3.0.0
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.6.0:java (default-cli) @ selenium-jupiter ---
[INFO] Selenium-Jupiter server listening on http://localhost:4042/wd/hub
```

* Using Selenium-Jupiter as a [fat-jar](https://github.com/bonigarcia/selenium-jupiter/releases/download/selenium-jupiter-3.0.0/selenium-jupiter-3.2.0-fat.jar). For instance:

```
> java -jar webdrivermanager-3.0.0-fat.jar server
[INFO] Selenium-Jupiter server listening on http://localhost:4042/wd/hub
```

When the Selenium-Jupiter server is up and running, it acts as a regular Selenium Server for Docker browsers (Chrome, Firefox,. Opera, Android), and its URL can be used in tests using regular Selenium's ``RemoteWebDriver`` objects.


## Documentation

You can find more details and examples on the [Selenium-Jupiter user guide].

## About

Selenium-Jupiter (Copyright &copy; 2017-2019) is a project by [Boni Garcia] licensed under [Apache 2.0 License]. Comments, questions and suggestions are always very [welcome][Selenium-Jupiter issues]!

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[Boni Garcia]: http://bonigarcia.github.io/
[Docker]: https://www.docker.com/
[Docker Engine]: https://www.docker.com/get-docker
[GitHub Repository]: https://github.com/bonigarcia/selenium-jupiter
[Logo]: http://bonigarcia.github.io/img/selenium-jupiter.png
[Selenium-Jupiter user guide]: https://bonigarcia.github.io/selenium-jupiter/
[Selenium-Jupiter issues]: https://github.com/bonigarcia/selenium-jupiter/issues
[Selenium Webdriver]: http://docs.seleniumhq.org/projects/webdriver/
[WebDriverManager]: https://github.com/bonigarcia/webdrivermanager
