package io.github.bonigarcia.seljup;

import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.platform.commons.util.Preconditions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The annotated method is a <em>test template</em> that should be repeated for each
 * <a href="https://bonigarcia.dev/selenium-jupiter/#template-tests">browser scenario</a>
 * with a configurable {@linkplain #name display name}.
 *
 * @see org.junit.jupiter.api.TestTemplate
 *
 * @see org.junit.jupiter.api.RepeatedTest
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
public @interface BrowserScenarioTest {

    /**
     * Placeholder for the {@linkplain TestInfo#getDisplayName display name} of
     * a {@code @BrowserScenarioTest} method: <code>{displayName}</code>
     */
    String DISPLAY_NAME_PLACEHOLDER = "{displayName}";

    /**
     * Placeholder for the {@linkplain BrowsersTemplate.Browser#getType() browser type} of
     * a {@code @BrowserScenarioTest} method: <code>{type}</code>
     */
    String TYPE_PLACEHOLDER = "{type}";

    /**
     * Placeholder for the {@linkplain BrowsersTemplate.Browser#getVersion() browser version} of
     * a {@code @BrowserScenarioTest} method: <code>{version}</code>
     */
    String VERSION_PLACEHOLDER = "{version}";

    /**
     * Placeholder for the {@linkplain BrowsersTemplate.Browser#getArguments() browser arguments} of
     * a {@code @BrowserScenarioTest} method: <code>{arguments}</code>
     */
    String ARGUMENTS_PLACEHOLDER = "{arguments}";

    /**
     * Placeholder for the {@linkplain BrowsersTemplate.Browser#getPreferences() browser preferences} of
     * a {@code @BrowserScenarioTest} method: <code>{preferences}</code>
     */
    String PREFERENCES_PLACEHOLDER = "{preferences}";

    /**
     * Placeholder for the {@linkplain BrowsersTemplate.Browser#getCapabilities() capabilities} of
     * a {@code @BrowserScenarioTest} method: <code>{capabilities}</code>
     */
    String CAPABILITIES_PLACEHOLDER = "{capabilities}";

    /**
     * Placeholder for the {@linkplain BrowsersTemplate.Browser#getRemoteUrl() remoteUrl} of
     * a {@code @BrowserScenarioTest} method: <code>{remoteUrl}</code>
     */
    String REMOTE_URL_PLACEHOLDER = "{remoteUrl}";

    /**
     * default display name pattern for a browser scenario test: {@value}
     */
    String DEFAULT_NAME = "{displayName} - {type} {version}";

    /**
     * The display name for each browser scenario test.
     *
     * <h4>Supported placeholders</h4>
     * <ul>
     * <li>{@link #DISPLAY_NAME_PLACEHOLDER}</li>
     * <li>{@link #TYPE_PLACEHOLDER}</li>
     * <li>{@link #VERSION_PLACEHOLDER}</li>
     * <li>{@link #ARGUMENTS_PLACEHOLDER}</li>
     * <li>{@link #PREFERENCES_PLACEHOLDER}</li>
     * <li>{@link #CAPABILITIES_PLACEHOLDER}</li>
     * <li>{@link #REMOTE_URL_PLACEHOLDER}</li>
     * </ul>
     *
     * <p>Defaults to {@link #DEFAULT_NAME}, resulting in
     * names such as {@code "chat button - chrome latest [--window-size=1280,720]"}
     *
     * <p>Alternatively, you can provide a custom display name, optionally
     * using the aforementioned placeholders.
     *
     * @return a custom display name; never blank or consisting solely of
     * whitespace
     */
    String name() default DEFAULT_NAME;

    /**
     * A utility class for formatting the display name of a browser scenario test.
     */
    final class NameFormatter {

        /**
         * The constructor is private to prevent instantiation.
         */
        private NameFormatter() {}

        /**
         * Formats the display name of a browser scenario test.
         *
         * @param namePattern   the name pattern from {@link BrowserScenarioTest#name()}
         * @param displayName   the display name from {@link ExtensionContext#getDisplayName()}
         * @param browser       the {@link BrowsersTemplate.Browser} from the {@link TestTemplateInvocationContext }
         * @return the formatted display name
         */
        public static String format(String namePattern, String displayName, BrowsersTemplate.Browser browser) {
            Preconditions.notNull(namePattern, "namePattern must not be null");
            Preconditions.notNull(displayName, "displayName must not be null");
            Preconditions.notNull(browser, "browser must not be null");
            String result = namePattern.trim();
            Preconditions.notBlank(result, "@BrowserScenarioTest must be declared with a non-empty name.");
            result = replacePlaceholders(result, DISPLAY_NAME_PLACEHOLDER, displayName);
            result = replacePlaceholders(result, TYPE_PLACEHOLDER, browser.getType());
            result = replacePlaceholders(result, VERSION_PLACEHOLDER, browser.getVersion());
            result = replacePlaceholders(result, ARGUMENTS_PLACEHOLDER, browser.getArguments(), Arrays::toString);
            result = replacePlaceholders(result, PREFERENCES_PLACEHOLDER, browser.getPreferences(), Arrays::toString);
            result = replacePlaceholders(result, CAPABILITIES_PLACEHOLDER, browser.getCapabilities(), Object::toString);
            return replacePlaceholders(result, REMOTE_URL_PLACEHOLDER, browser.getRemoteUrl());
        }

        /**
         * Null-safe placeholder replacement in the name pattern with the given string value. If the value is null,
         * replacement is skipped.
         *
         * @param namePattern the (not-nullable) pattern for string replacements
         * @param placeholder the (not-nullable) placeholder to be replaced
         * @param value the (nullable) string value to replace with which to replace the placeholder
         * @return the (non-null) string pattern with placeholders replaced by the given value
         */
        private static String replacePlaceholders(String namePattern, String placeholder, String value) {
            return replacePlaceholders(namePattern, placeholder, value, UnaryOperator.identity());
        }

        /**
         * Null-safe placeholder replacement in the name pattern with the given value transformed with a mapping
         * function. If the value is null, replacement is skipped.
         *
         * @param namePattern the (not-nullable) pattern for string replacements
         * @param placeholder the (not-nullable) placeholder to be replaced
         * @param value the (nullable) string value to replace with which to replace the placeholder
         * @param mapper a mapping function to transform the value into a suitable string
         * @return the (non-null) string pattern with placeholders replaced by the given value
         * @param <T> the generic type of the value
         */
        private static <T> String replacePlaceholders(String namePattern, String placeholder, T value, Function<T, String> mapper) {
            if (value != null) {
                return namePattern.replace(placeholder, mapper.apply(value));
            } else {
                return namePattern;
            }
        }

    }

}
