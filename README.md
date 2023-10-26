[![Maven Central](https://img.shields.io/maven-central/v/io.github.bonigarcia/selenium-jupiter.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3Aio.github.bonigarcia%20a%3Aselenium-jupiter)
[![Build Status](https://github.com/bonigarcia/selenium-jupiter/workflows/build/badge.svg)](https://github.com/bonigarcia/selenium-jupiter/actions)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=io.github.bonigarcia:selenium-jupiter&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=io.github.bonigarcia%3Aselenium-jupiter)
[![codecov](https://codecov.io/gh/bonigarcia/selenium-jupiter/branch/master/graph/badge.svg)](https://codecov.io/gh/bonigarcia/selenium-jupiter)
[![badge-jdk](https://img.shields.io/badge/jdk-17-green.svg)](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![License badge](https://img.shields.io/badge/license-Apache2-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Backers on Open Collective](https://opencollective.com/selenium-jupiter/backers/badge.svg)](#backers)
[![Sponsors on Open Collective](https://opencollective.com/selenium-jupiter/sponsors/badge.svg)](#sponsors)
[![Support badge](https://img.shields.io/badge/stackoverflow-selenium_jupiter-green.svg?logo=stackoverflow)](https://stackoverflow.com/questions/tagged/selenium-jupiter?sort=newest)
[![Twitter Follow](https://img.shields.io/twitter/follow/boni_gg.svg?style=social)](https://twitter.com/boni_gg)

# [![][Logo]][Selenium-Jupiter]

[Selenium-Jupiter] is an open-source Java library that implements a [JUnit 5] extension for developing [Selenium WebDriver] tests. Selenium-Jupiter uses several features of the Jupiter extension (such as parameters resolution, test templates, or conditional test execution). Thanks to this, the resulting Selenium-Jupiter tests follow a minimalist approach (i.e., the required boilerplate code for WebDriver is reduced) while providing a wide range of advanced features for end-to-end testing.

## Documentation
You can find the complete documentation of Selenium-Jupiter [here][Selenium-Jupiter]. This site contains all the features, examples, and configuration capabilities of Selenium-Jupiter.

## Local browsers
Selenium-Jupiter can be used to control local browsers programmatically using Selenium WebDriver. To do that, we need to specify the flavor of the browser to be used by declaring `WebDriver` parameters in tests or constructors. For instance, we declare a `ChromeDriver` parameter to use Chrome, `FirefoxDriver` for Firefox, and so on. For instance:  

```java
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
class ChromeTest {

    @Test
    void test(ChromeDriver driver) {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.getTitle()).contains("Selenium WebDriver");
    }

}
```

Internally, Selenium-Jupiter uses [WebDriverManager] to manage the WebDriver binaries (i.e., chromedriver, geckodriver,  etc.) required to use local browsers.

### Browsers in Docker containers
Selenium-Jupiter allows using browsers in [Docker] containers very easily. The only requirement is to get installed [Docker Engine] in the machine running the tests. The following example shows a test using this feature. Internally, it pulls the image from [Docker Hub], starts the container, and instantiates the WebDriver object to use it. This example also enables the recording of the browser session and remote access using [noVNC]:

```java
import static io.github.bonigarcia.seljup.BrowserType.CHROME;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
class DockerChromeTest {

    @Test
    void testChrome(@DockerBrowser(type = CHROME) WebDriver driver) {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.getTitle()).contains("Selenium WebDriver");
    }

}
```

### Conditional tests
Selenium-Jupiter provides the class-level annotation @EnabledIfBrowserAvailable to skip tests conditionally depending on the availability of local browsers. For example:

```java
import static io.github.bonigarcia.seljup.Browser.SAFARI;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.safari.SafariDriver;

import io.github.bonigarcia.seljup.EnabledIfBrowserAvailable;
import io.github.bonigarcia.seljup.SeleniumJupiter;

@EnabledIfBrowserAvailable(SAFARI)
@ExtendWith(SeleniumJupiter.class)
class SafariTest {

    @Test
    void test(SafariDriver driver) {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.getTitle()).contains("Selenium WebDriver");
    }

}

```

### Test templates
Test templates are a special kind of test in which the same test logic is executed several times according to some custom data. In Selenium-Jupiter, the data to feed a test template is referred to as the _browser scenario_ (a JSON file by default).

```java
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;

@ExtendWith(SeleniumJupiter.class)
class TemplateTest {

    @TestTemplate
    void templateTest(WebDriver driver) {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.getTitle()).contains("Selenium WebDriver");
    }

}
```

... and the browser scenario is:

```yaml
{
   "browsers": [
      [
         {
            "type": "chrome-in-docker",
            "version": "latest"
         }
      ],
      [
         {
            "type": "chrome-in-docker",
            "version": "latest-1"
         }
      ],
      [
         {
            "type": "chrome-in-docker",
            "version": "beta"
         }
      ],
      [
         {
            "type": "chrome-in-docker",
            "version": "dev"
         }
      ]
   ]
}
```

## Support
Selenium-Jupiter is part of [OpenCollective], an online funding platform for open and transparent communities. You can support the project by contributing as a backer (i.e., a personal [donation] or [recurring contribution]) or as a [sponsor] (i.e., a recurring contribution by a company).

### Backers
<a href="https://opencollective.com/selenium-jupiter" target="_blank"><img src="https://opencollective.com/selenium-jupiter/backers.svg?width=890"></a>

### Sponsors
<a href="https://opencollective.com/selenium-jupiter/sponsor/0/website" target="_blank"><img src="https://opencollective.com/selenium-jupiter/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/selenium-jupiter/sponsor/1/website" target="_blank"><img src="https://opencollective.com/selenium-jupiter/sponsor/1/avatar.svg"></a>
<a href="https://opencollective.com/selenium-jupiter/sponsor/2/website" target="_blank"><img src="https://opencollective.com/selenium-jupiter/sponsor/2/avatar.svg"></a>

Alternatively, you can acknowledge my work by buying me a coffee:

<p><a href="https://www.buymeacoffee.com/bonigarcia"> <img align="left" src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" height="50" width="210"/></a></p><br><br>

## About
Selenium-Jupiter (Copyright &copy; 2017-2023) is a project created and maintained by [Boni García] and licensed under the terms of the [Apache 2.0 License].

If you like my work, please consider nominating me for the [GitHub Stars](https://stars.github.com/nominate/) program.

<p align="center"><a href="https://stars.github.com/nominate/"> <img src="https://avatars.githubusercontent.com/u/61242156?s=200&v=4" width="100"/></a></p>

[Logo]: https://bonigarcia.dev/img/seljup.png
[Selenium-Jupiter]: https://bonigarcia.dev/selenium-jupiter/
[JUnit 5]: https://junit.org/junit5/docs/current/user-guide/
[Selenium WebDriver]: https://www.selenium.dev/documentation/webdriver/
[WebDriverManager]: https://github.com/bonigarcia/webdrivermanager
[Docker]: https://www.docker.com/
[Docker Engine]: https://www.docker.com/get-docker
[Docker Hub]: https://hub.docker.com/
[noVNC]: https://novnc.com/
[OpenCollective]: https://opencollective.com/selenium-jupiter
[donation]: https://opencollective.com/selenium-jupiter/donate
[recurring contribution]: https://opencollective.com/selenium-jupiter/contribute/backer-8132/checkout
[sponsor]: https://opencollective.com/selenium-jupiter/contribute/sponsor-8133/checkout
[Boni Garcia]: https://bonigarcia.dev/
[Apache 2.0 License]: https://www.apache.org/licenses/LICENSE-2.0
