package io.github.bonigarcia.seljup.test.singlessession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.seljup.SeleniumJupiter;
import io.github.bonigarcia.seljup.SingleSession;

@TestInstance(PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SingleSession
@Disabled("To avoid breaking CI build")
class ScreenshotNamedWithClassTest {

    @RegisterExtension
    static SeleniumJupiter seleniumJupiter = new SeleniumJupiter();

    RemoteWebDriver driver;

    @BeforeAll
    void setup() {
        seleniumJupiter.getConfig().enableScreenshotWhenFailure();
        seleniumJupiter.getConfig().takeScreenshotAsPng();
    }

    @BeforeAll
    void resolveWebDriver(ChromeDriver webDriver) {
        this.driver = webDriver;
    }

    @Test
    void shouldFailAndCreateScreenshotTest() {
        driver.get("https://bonigarcia.dev/selenium-webdriver-java/");
        assertThat(driver.getTitle()).contains("AAA");
    }

}
