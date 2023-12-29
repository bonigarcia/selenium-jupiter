# Changelog

## [5.0.1] - 2023-12-30
### Added
- Include args field in DockerBrowser annotation


## [5.0.0] - 2023-10-27
### Added
- Capabilities handler accepts also boolean capabilities (#409)

### Changed
- Use Java 17 (required for the use of Selenide dependency)
- Bump to WebDriverManager 5.6.7 (fix issues related to use host as Docker network)

### Fixed
- Use of Options.ofNullable in annotation reader logic (#410)


## [4.3.7] - 2023-08-01
### Changed
- Bump to WebDriverManager 5.5.3 (fix issues related to CfT request)


## [4.3.6] - 2023-07-19
### Changed
- Bump to WebDriverManager 5.4.1 (use last good versions CfT endpoint only when driver version is unknown)


## [4.3.5] - 2023-06-21
### Changed
- Bump to Java 11 (required in Selenium as of September 30, 2023). See https://www.selenium.dev/blog/2023/java-8-support/
- Bump to WebDriverManager 5.4.0 (Use CfT JSON endpoints for version discovering as of chromedriver 115)


## [4.3.4] - 2023-05-12
### Changed
- Bump to WebDriverManager 5.3.3 (fix monitoring features)


## [4.3.3] - 2023-01-19
### Changed
- Bump to WebDriverManager 5.3.2 (fix issues related to driver manager in macOS M1 and Edge in Docker)


## [4.3.2] - 2022-11-4
### Changed
- Bump to WebDriverManager 5.3.1 (fix latest issues related to driver management)

### Removed
- Remove references of OperaOptions (removed as of Selenium 4.5.0)


## [4.3.1] - 2022-08-13
### Changed
- Bump to WebDriverManager 5.3.0 (avoids 403 error and enhance log gathering in Firefox)


## [4.3.0] - 2022-08-13
### Added
- Integrate with BrowserWatcher to provide monitoring capabilities
- Separate recording output folders per class not only for surefire-configuration (#286)

### Removed
- Documentation in EPUB format


## [4.2.0] - 2022-06-26
### Added
- Add new custom annotation called @Opera for Opera browsers

### Changed
- Bump to WebDriverManager 5.2.1 (required for Selenium 4.3.0)


## [4.1.0] - 2022-02-18
### Added
- Support DevTools in the parameter resolution mechanism
- Include @EnabledIfDriverUrlOnline to conditionally skip tests
- Include @EnabledIfDockerAvailable to disable test conditionally depending on Docker
- Include support for Appium 2.x in Selenium-Jupiter 4.x
- Include support for Selenide in Selenium-Jupiter 4.x
- Include methods in SeleniumJupiter class to get the Docker noVNC URL

### Changed
- Include timestamp in screenshot filename

### Fixed
- Config option enableRecordingWhenFailure does not work for TestTemplate approach (issue #205)

## [4.0.1] - 2021-08-31
### Changed
- Downgrade to Java 8


## [4.0.0] - 2021-08-31
### Added
- New documentation: https://bonigarcia.dev/selenium-jupiter/ (sources: AsciiDoc, generated: HTML, PDF, EPUB)
- Include @EnabledIfBrowserAvailable annotation to disable tests conditionally
- Use WebDriverManager 5 for managing browsers in Docker containers
- Allow to specify language and timezone using @DockerBrowser annotation
- Enhance test template support (issue #45)
- Include support for Safari as dockerized browser (WebKit engine)
- Make screenshots and recordings in AfterTestExecutionCallback and quit browser in AfterEachCallback/AfterAllCallback (issue #61)
- Allow making recordings when test failure

### Changed
- Use selenium-java as provided dependency
- Use Java 11
- Use Selenium 4 in tests
- Use AssertJ for assertions in tests

### Fixed
- Improve support for parallel execution (issue #80)
- Improve support for single session (issue #181)

### Removed
- Remove support for phantomjsdriver, selenide, htmlunit-driver, and Appium java-client (not compatible with Selenium 4)
- Remove support to Genymotion
- Remove support for docker-android (use chrome-mobile instead)
- Remove all configuration keys (and API methods) related to custom Docker management
- Remove support for CLI and server modes (moved features to WebDriverManager)


## [3.4.0] - 2021-01-26
### Added
- Support for Edge (for Linux) in Docker containers (pulled from Docker Hub)
- Support for standalone mode in Docker (catch exception when pulling images, issue #94)

### Changed
- Replace spotify/docker-client with docker-java/docker-java (PR #97)


### Fixed
- Use empty string in remote capabilities for latest version (issue #54)

### Removed
- Remove request entity processing config key (incompatible with docker-java)


## [3.3.5] - 2020-09-11
### Added
- Include config key sel.jup.docker.request.entity.processing (issue #82)
- Let @SingleSession supports meta-present (issue #81)

### Changed
- Use SeleniumJupiter class as JUnit 5 extension point
- Bump dependencies (WebDriverManager 4.2.0, Selenide 5.14.2 ,Appium java-client 7.3.0)

### Fixed
- Fix recording not deleted when using different outputFolder (issue #76)
- Fix creating WebDriver hanging when using @Nested JUnit5 annotation (issue #74)


## [3.3.4] - 2020-01-19
### Added
- Include support for Chromium (checking binary from Chrome options)

### Changed
- Bump dependencies (WebDriverManager 3.8.1)

### Fixed
- Fix Edge support for Chromium-based version (recently stable)
- Include default value for property sel.jup.recording.when.failure


## [3.3.3] - 2020-01-14
### Added
- Add support for additional specific drivers (issue #67, PR #68)

### Changed
- Bump dependencies (selenide 5.6.0, WebDriverManager 3.8.0)

### Fixed
- Fix parsing of wildcards in volumes for Docker containers


## [3.3.2] - 2019-10-28
### Added
- Use latest version of guava (28.1-jre)

### Changed
- Bump aerokube/selenoid to version 1.9.3
- Use binary setup for Opera in Docker only in version 62.0
- Change example extension for CRX3 compatible (required as of Chrome 78)

### Fixed
- PR #65: Add class name to screenshot filename if method name unavailable

### Removed
- Remove binary setup for Opera (not working anymore as of 64.0 in Windows)


## [3.3.1] - 2019-09-18
### Added
- Automatic binary setup for Edge dev (Chromium-based version)
- Automatic binary setup for Opera based on default values for Win, Linux, and Mac
- Include required setup for Opera (version 62) in Docker
- Include timeout for Selenium-Jupiter server (default 180 seconds)

### Changed
- Change stop timeout for Docker containers to 5 seconds
- Change startup timeout for Docker containers to 3 minutes
- Increase default value of TTL to 86400 seconds (i.e. one day)
- Bump WebDriverManager to version 3.7.1
- Bump JUnit to version 5.5.2
- Bump Selenide to version 5.3.1
- Bump javalin to 2.8.0 and okhttp to 3.14.2
- Bump htmlunit-driver to 2.36.0 and include it as compile dependency

### Fixed
- Fix issue #65 (make screenshots when SingleSession in enabled)
- Fix attachments Jenkins support for template tests
- Fix edge and iexplorer in Docker support


## [3.3.0] - 2019-08-02
### Added
- Integration with Selenide (https://selenide.org/) through SelenideDriver objects
- Include @SelenideConfiguration annotation (field/parameter-level) for Selenide config
- Allow @DockerBrowser for SelenideDriver objects
- Allow @DriverUrl and @DriverCapabilities for SelenideDriver objects

### Changed
- Bump JUnit to version 5.5.1


## [3.2.2] - 2019-07-19
### Added
- Improve Appium Logging Configuration (PR #64)

### Changed
- Bump WebDriverManager to version 3.6.2 (fix Edge support)
- Bump spotify:docker-client version to 8.16.0
- Change changelog format to Markdown


## [3.2.1] - 2019-05-14
### Added
- Quit WebDriver object depending on a boolean value (fix #55)
- Allow inherited methods to use docker browsers (PR #58)
- Add sel.jup.docker.host config parameter (PR #57)

### Changed
- Bump WebDriverManager to version 3.5.0 (fix Edge support)
- Bump spotify:docker-client version to 8.15.2 (issue #40)


## [3.2.0] - 2019-04-21
### Added
- Support Microsoft Edge and Internet Explorer in Docker containers
- Improve Genymotion SAAS support
- Allow to configure volumes form @DockerBrowser annotation
- Include class-level annotation @SingleSession to allow run tests in the same session (issue #52)

### Fixed
- Fix remote Docker daemon usage


## [3.1.1] - 2019-02-19
### Added
- Include configuration keys for Android screen (width, height, depth)

### Fixed
- Fix #46 Bump several dependencies versions
- Fix #47 Get parameter index from context
- Fix #48 Add timeout for Android start in docker container
- Fix #50 Add possibility to capture log files from Android
- Set default Android version as 9.0

### Removed
- Remove support for Genymotion PaaS due to lack of documentation


## [3.1.0] - 2019-01-23
### Added
- Read @DriverUrl annotation also for @TestTemplate
- Add support for Genymotion (SaaS, PaaS) in Android devices
- Include "internet explorer" label for test templates (issue #44)

### Changed
- Maven dependency: com.spotify:docker-client 8.15.0
- Docker hub dependency: butomo1989/docker-android-x86*:1.5-p6 (using old versions due to bugs in newer)
- Set default Android version as 8.1

### Fixed
- Fix docker-android boolean parameters as envs


## [3.0.0] - 2018-01-07
### Added
- Selenium-Jupiter server
- Thread-safe handling to allow concurrent tests (JUnit 5.3)
- Remove global instance of configuration object (use @RegisterExtension instead)
- Allow using @Options annotation for different browsers (ChromeOptions, FirefoxOptions, etc.)
- Include parameter in test templates for Selenium Server URL (issue #17)
- RemoteWebDriver creation using timeout and poll time
- Use Java preferences to store latest versions of Docker browsers (Chrome, Firefox, Opera) and pulled images

### Changed
- Maven dependency: org.seleniumhq.selenium:selenium-java 3.141.59
- Maven dependency: io.github.bonigarcia:webdrivermanager 3.2.0
- Maven dependency: io.appium:java-client 7.0.0
- Maven dependency: com.spotify:docker-client 8.14.5
- Maven dependency: com.codeborne:phantomjsdriver 1.4.4
- Docker hub dependency: aerokube/selenoid:1.8.4
- Docker hub dependency: butomo1989/docker-android-x86*:1.5-p6

### Fixed
- Fix issue #22 (template-tests with multiple test methods per class)
- Fix issue #20 (@Options annotation is ignored when there is a superclass)
- Fix issue #36 (when DockerDriverHandler stats container with Android, it doesn't pass proxy settings)
- Fix issue #31 (when sel.jup.browser.list.from.docker.hub=true legacy docker images are not available)
- Fix issue #29 (use thread-safe collections for concurrent tests)


## [2.2.0] - 2018-07-04
### Added
- Include Android browsers in Docker using butomo1989 images
- Include Internet Explorer as configurable browser
- Include META-INF information for ServiceLoader mechanism

### Changed
- Maven dependency: org.seleniumhq.selenium:selenium-java 3.13.0
- Maven dependency: io.github.bonigarcia:webdrivermanager 2.2.4
- Maven dependency: io.appium:java-client 6.1.0
- Maven dependency: com.spotify:docker-client 8.11.7
- Maven dependency: com.codeborne:phantomjsdriver 1.4.4
- Docker hub dependency: aerokube/selenoid:1.6.2
- Docker hub dependency: butomo1989/docker-android-x86*:0.9-p5 (Linux)
- Docker hub dependency: butomo1989/docker-android-arm*:0.9-p5 (Mac and Win)


## [2.1.1] - 2018-04-09
### Added
- Include configuration key for Selenium Server URL
- Set unlimited timeout for browsers in interactive sessions

### Changed
- Maven dependency: io.github.bonigarcia:webdrivermanager 2.2.1
- Docker hub dependency: aerokube/selenoid:1.6.0
- Get Docker host address using gateway only in Linux (issue #7)

### Removed
- Remove sel.jup.docker.default.host configuration key


## [2.1.0] - 2018-03-31
### Added
- Use beta/unstable Docker containers for Chrome and Firefox
- Configuration manager: SeleniumJupiter.config()
- Interactive mode (from shell) to get remote sessions (VNC)
- Config key for Docker API version
- Config key for Docker network (bridge by default)
- Config key for path and tmpfs
- Config key for parallel browser list
- Improve compatibility of Docker support for Linux, Windows, and Mac
- Improve support for dependency injection in constructor parameters

### Changed
- Maven dependency: org.seleniumhq.selenium:selenium-java 3.11.0
- Maven dependency: io.github.bonigarcia:webdrivermanager 2.2.0
- Maven dependency: io.appium:java-client 5.0.4
- Maven dependency: com.spotify:docker-client 8.11.2
- Maven dependency: com.codeborne:phantomjsdriver 1.4.4
- Docker hub dependency: aerokube/selenoid:1.5.3
- Docker hub dependency: selenoid/vnc:chrome_*, selenoid/vnc:firefox_*, selenoid/vnc:opera_*
- Docker hub dependency: elastestbrowsers/chrome:beta, elastestbrowsers/chrome:unstable, elastestbrowsers/firefox:beta, elastestbrowsers/firefox:nightly 
- Replace com.github:docker-java library with com.spotify:docker-client


## [2.0.0] - 2018-01-15
### Added
- Include annotations @Arguments, @Binary, @Extensions, @Options, and @Preferences
- Support for dockerized browsers (Chrome, Firefox, Opera)
- Use of Selenoid docker images for dockerized browsers (http://aerokube.com/selenoid/latest/)
- Support for live session view of dockerized sessions using VNC
- Include config key sel.jup.docker.vnc to activate VNC in Docker containers
- Support for performance test asking for lists of dockerized browsers
- Read from Docker Hub the dockerized browsers images (https://hub.docker.com//v2/repositories/selenoid/vnc/tags/?page_size=1024)
- Support for recording dockerized sessions in MP4 format
- Include configuration keys for screen resolution, size, and frame rate
- Include configuration key for browser container timeout
- Support for test template based on JSON browser scenarios
- Write browser screenshot using config key (sel.jup.screenshot.at.the.end.of.tests=true|false|whenfailure)
- Select format for screenshot using config key (sel.jup.screenshot.format=base64|png|base64andpng)
- Select output folder (used for screenshots and images) using config key (sel.jup.output.folder=/abs/path|./rel/path|surefire-reports)
- Seamless integration with Jenkins attachment plugin (https://wiki.jenkins.io/display/JENKINS/JUnit+Attachments+Plugin) using surefire-reports
- Include labels "latest" and "latest-*" for version of browsers in Docker

### Changed
- Use of io.github.bonigarcia:webdrivermanager version 2.1.0
- Use of io.appium:java-client version 5.0.4
- Use of com.github.docker-java:docker-java version 3.0.14
- Use of com.codeborne:phantomjsdriver version 1.4.3

### Removed
- Remove annotations @DriverOptions and @Option


## [1.2.0] - 2017-12-13
### Added
- Seek for annotated fields (DriverOptions, etc) in test superclass(es) [issue #2]
- Log Base64 screenshots of browser session(s) when test fails
- Include configuration key (sel.jup.exception.when.no.driver) to raise exception or not when failure
- Use SonarCloud to keep a good level of internal code quality
- Use Codecov to keep a good level of code coverage

### Changed
- Use of org.seleniumhq.selenium:selenium-java version 3.8.1
- Use of io.github.bonigarcia:webdrivermanager version 2.0.1
- Use of io.appium:java-client version 5.0.4
- Improvement of test suite


## [1.1.2] - 2017-07-17
### Changed
- Use of JUnit 5.0.0 (GA) for tests
- Use of io.github.bonigarcia:webdrivermanager version 1.7.2
- Use of io.appium:java-client version 5.0.3


## [1.1.1] - 2017-09-04
### Changed
- Use of org.seleniumhq.selenium:selenium-java version 3.5.3
- Use of io.appium:java-client version 5.0.2


## [1.1.0] - 2017-08-03
### Added
- Support for Appium (AppiumDriver<T extends WebElement>)

### Changed
- Use of io.appium:java-client version 5.0.0-BETA9


## [1.0.0] - 2017-07-12
### Added
- Use of JUnit 5 dependency injection to use WebDriver objects as parameter in Jupiter tests
- Support for ChromeDriver, FirefoxDriver, EdgeDriver, OperaDriver, SafariDriver, HtmlUnitDriver, PhantomJSDriver, and InternetExplorerDriver
- Integration with WebDriverManager (1.7.1) to manage WebDriver binaries (chromedriver, geckodriver, etc)
- Integration with selenium-java (3.4.0)
- Provide the annotation @DriverCapabilities, @DriverCapabilities, and @DriverUrl to specify options/capabilities to instatiate WebDriver objects
- Allow to use these annotations at parameter level and also at field level (global options/capabilities)
- Rich test suite running on Travis CI
- AsciiDoc documentation, generated to HTML and PDF with maven-asciidoctor-plugin (mvn site)
- Reuse tests as examples in documentation (using AsciiDoc tags)
- Publication of documentation on gh-pages branch (https://bonigarcia.github.io/selenium-jupiter/)
