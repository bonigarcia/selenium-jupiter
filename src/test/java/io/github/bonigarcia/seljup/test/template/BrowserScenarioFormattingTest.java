package io.github.bonigarcia.seljup.test.template;

import com.google.gson.internal.LinkedTreeMap;
import io.github.bonigarcia.seljup.BrowserBuilder;
import io.github.bonigarcia.seljup.BrowserScenarioTest;
import io.github.bonigarcia.seljup.BrowsersTemplate.Browser;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrowserScenarioFormattingTest {

    @Test
    void replaceAllPlaceholders() {
        LinkedTreeMap<String, String> capabilities = new LinkedTreeMap<>();
        capabilities.put("custom:cap-1", "value-1");
        capabilities.put("custom:cap-2", "custom-value-2");
        capabilities.put("custom:cap-3", "true");
        Browser browser = new BrowserBuilder("chrome-in-docker")
                .version("latest")
                .arguments(new String[]{"--disable-gpu", "--no-sandbox"})
                .preferences(new String[]{"media.navigator.permission.disabled=true", "media.navigator.streams.fake=true"})
                .capabilities(capabilities)
                .remoteUrl("http://localhost:4444/")
                .build();
        String result = BrowserScenarioTest.NameFormatter.format(
                "{displayName}, {type}, {version}, {arguments}, {preferences}, {capabilities}, {remoteUrl}",
                "Sample Test",
                browser
        );
        assertEquals("""
                Sample Test, chrome-in-docker, latest, [--disable-gpu, --no-sandbox], \
                [media.navigator.permission.disabled=true, media.navigator.streams.fake=true], \
                {custom:cap-1=value-1, custom:cap-2=custom-value-2, custom:cap-3=true}, http://localhost:4444/""", result);
    }

    @Test
    void skipNullBrowserAttributes() {
        Browser browser = new BrowserBuilder(null)
                .version(null)
                .arguments(null)
                .preferences(null)
                .capabilities(null)
                .remoteUrl(null)
                .build();
        String result = BrowserScenarioTest.NameFormatter.format(
                "{displayName}, {type}, {version}, {arguments}, {preferences}, {capabilities}, {remoteUrl}",
                "Sample Test",
                browser
        );
        assertEquals("Sample Test, {type}, {version}, {arguments}, {preferences}, {capabilities}, {remoteUrl}", result);
    }

    @Test
    void throwExceptionWhenNamePatternIsNull() {
        Browser browser = new BrowserBuilder("chrome").build();
        assertThrows(PreconditionViolationException.class, () -> BrowserScenarioTest.NameFormatter.format(
                null,
                "Sample Test",
                browser
        ));
    }

    @Test
    void throwExceptionWhenDisplayNameIsNull() {
        Browser browser = new BrowserBuilder("chrome").build();
        assertThrows(PreconditionViolationException.class, () -> BrowserScenarioTest.NameFormatter.format(
                "{displayName} - {type} {version}",
                null,
                browser
        ));
    }

    @Test
    void throwExceptionWhenBrowserIsNull() {
        assertThrows(PreconditionViolationException.class, () -> BrowserScenarioTest.NameFormatter.format(
                "{displayName} - {type} {version}",
                "Sample Test",
                null
        ));
    }

    @Test
    void throwExceptionWhenNamePatternIsEmpty() {
        Browser browser = new BrowserBuilder("chrome").build();
        assertThrows(PreconditionViolationException.class, () -> BrowserScenarioTest.NameFormatter.format(
                "   ",
                "Sample Test",
                browser
        ));
    }

}
