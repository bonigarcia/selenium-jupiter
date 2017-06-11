# selenium-jupiter [![][Logo]][GitHub Repository]

*selenium-jupiter* is a Jupiter extension aimed to ease the use of Selenium WebDriver in JUnit 5 tests. This library is open source, released under the terms of [Apache 2.0 License].

## Usage

In order to include *selenium-jupiter* in a Maven project, first add the following dependency to your `pom.xml` (Java 8 required):

```xml
<dependency>
	<groupId>io.github.bonigarcia</groupId>
	<artifactId>selenium-jupiter</artifactId>
	<version>1.0.0</version>
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

Internally, eclipse-juniter uses [WebDriverManager] to manage the required WebDriver binaries (i.e. *chromedriver*, *geckodriver*,  *operadriver*, and so on).

## About

selenium-jupiter (Copyright &copy; 2017) is a personal project of [Boni Garcia] licensed under [Apache 2.0 License]. Comments, questions and suggestions are always very [welcome][selenium-jupiter issues]!

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[Boni Garcia]: http://bonigarcia.github.io/
[GitHub Repository]: https://github.com/bonigarcia/selenium-jupiter
[Logo]: http://bonigarcia.github.io/img/selenium-jupiter.png
[selenium-jupiter issues]: https://github.com/bonigarcia/selenium-jupiter/issues
[Selenium Webdriver]: http://docs.seleniumhq.org/projects/webdriver/
[WebDriverManager]: https://github.com/bonigarcia/webdrivermanager
