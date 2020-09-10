package io.github.bonigarcia.seljup.test.docker;

import static com.codeborne.selenide.Selenide.$;
import static io.github.bonigarcia.seljup.BrowserType.CHROME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import io.github.bonigarcia.seljup.DockerBrowser;
import io.github.bonigarcia.seljup.SeleniumJupiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.remote.RemoteWebDriver;

@ExtendWith(SeleniumJupiter.class)
class DockerChromeJupiterWithNestedTest {

  @BeforeEach
  void openSite(@DockerBrowser(type = CHROME) RemoteWebDriver driver) {
    WebDriverRunner.setWebDriver(driver);
    Selenide.open("https://bonigarcia.github.io/selenium-jupiter/");
  }

  @Nested
  class MyNestedTest {

    @BeforeEach
    void checkTitle() {
      assertThat(Selenide.title(),
          containsString("JUnit 5 extension for Selenium"));
    }

    @Nested
    class MoreNested {

      @BeforeEach
      void checkToc() {
        $("#toc").should(Condition.exist);
      }

      @Test
      void quickReference() {
        $("a[href=\"#quick-reference\"]").should(Condition.exist);
      }
    }
  }

}
