[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.bonigarcia/selenium-jupiter/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.bonigarcia/selenium-jupiter)
[![Build Status](https://travis-ci.org/bonigarcia/selenium-jupiter.svg?branch=master)](https://travis-ci.org/bonigarcia/selenium-jupiter)
[![Quality Gate](https://sonarcloud.io/api/badges/gate?key=io.github.bonigarcia:selenium-jupiter)](https://sonarcloud.io/dashboard/index/io.github.bonigarcia:selenium-jupiter)
[![codecov](https://codecov.io/gh/bonigarcia/selenium-jupiter/branch/master/graph/badge.svg)](https://codecov.io/gh/bonigarcia/selenium-jupiter)
[![License badge](https://img.shields.io/badge/license-Apache2-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Support badge]( https://img.shields.io/badge/support-sof-green.svg)](http://stackoverflow.com/questions/tagged/selenium-jupiter)
[![Twitter Follow](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/boni_gg)

# selenium-jupiter [![][Logo]][GitHub Repository]

*selenium-jupiter* is a JUnit 5 extension aimed to ease the use of Selenium WebDriver in Jupiter tests. This library is open source, released under the terms of [Apache 2.0 License].

## Usage

In order to include *selenium-jupiter* in a Maven project, first add the following dependency to your `pom.xml` (Java 8 required):

```xml
<dependency>
	<groupId>io.github.bonigarcia</groupId>
	<artifactId>selenium-jupiter</artifactId>
	<version>1.2.0</version>
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
public class ChromeJupiterTest {

    @Test
    public void test1(ChromeDriver chrome) {
    	// use chrome in this test
    }

    @Test
    public void test2(FirefoxDriver firefox) {
    	// use firefox in this test
    }

}
```

Internally, eclipse-jupiter uses [WebDriverManager] to manage the required WebDriver binaries (i.e. *chromedriver*, *geckodriver*,  *operadriver*, and so on).

## Documentation

You can find more details about this extension taking a look to the [selenium-jupiter user guide].


## About

selenium-jupiter (Copyright &copy; 2017) is a project by [Boni Garcia] licensed under [Apache 2.0 License]. Comments, questions and suggestions are always very [welcome][selenium-jupiter issues]!

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[Boni Garcia]: http://bonigarcia.github.io/
[GitHub Repository]: https://github.com/bonigarcia/selenium-jupiter
[Logo]: http://bonigarcia.github.io/img/selenium-jupiter.png
[selenium-jupiter user guide]: https://bonigarcia.github.io/selenium-jupiter/
[selenium-jupiter issues]: https://github.com/bonigarcia/selenium-jupiter/issues
[Selenium Webdriver]: http://docs.seleniumhq.org/projects/webdriver/
[WebDriverManager]: https://github.com/bonigarcia/webdrivermanager
