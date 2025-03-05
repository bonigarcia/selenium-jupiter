/*
 * (C) Copyright 2017 Boni Garcia (https://bonigarcia.github.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.bonigarcia.seljup;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;

/**
 * Options/capabilities reader from annotated parameters or test instance to the
 * proper type (ChromeOptions, FirefoxOptions, Capabilities, etc).
 *
 * @author Boni Garcia
 * @since 1.0.0
 */
public class AnnotationsReader {

    static final Logger log = getLogger(lookup().lookupClass());

    public Optional<Capabilities> getCapabilities(Parameter parameter,
            Optional<Object> testInstance) {
        Optional<Capabilities> out = empty();
        try {
            DriverCapabilities driverCapabilities = parameter
                    .getAnnotation(DriverCapabilities.class);

            Capabilities capabilities = null;
            if (driverCapabilities != null) {
                // Search first DriverCapabilities annotation in parameter
                capabilities = new DesiredCapabilities();
                for (String capability : driverCapabilities.value()) {
                    Optional<List<Object>> keyValue = getKeyValue(capability);
                    if (keyValue.isPresent()) {
                        ((DesiredCapabilities) capabilities).setCapability(
                                keyValue.get().get(0).toString(),
                                keyValue.get().get(1));
                    }
                }
                out = of(capabilities);
            } else {
                // If not, search DriverCapabilities in any field
                Optional<Object> annotatedField = seekFieldAnnotatedWith(
                        testInstance, DriverCapabilities.class);
                if (annotatedField.isPresent()) {
                    capabilities = (Capabilities) annotatedField.get();
                    out = of(capabilities);
                }
            }
        } catch (Exception e) {
            log.warn("Exception getting capabilities", e);
        }
        return out;
    }

    public Optional<URL> getUrl(Parameter parameter,
            Optional<Object> testInstance, String seleniumServerUrl) {
        Optional<URL> out = empty();

        try {
            if (seleniumServerUrl != null && !seleniumServerUrl.isEmpty()) {
                out = of(new URL(seleniumServerUrl));
            } else {
                Object urlValue = null;
                DriverUrl driverUrl = parameter.getAnnotation(DriverUrl.class);
                if (driverUrl != null) {
                    // Search first DriverUrl annotation in parameter
                    urlValue = driverUrl.value();
                    out = of(new URL(urlValue.toString()));
                } else {
                    // If not, search DriverUrl in any field
                    Optional<Object> annotatedField = seekFieldAnnotatedWith(
                            testInstance, DriverUrl.class);
                    if (annotatedField.isPresent()) {
                        urlValue = annotatedField.get();
                        out = of(new URL(urlValue.toString()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Exception getting URL", e);
        }
        return out;
    }

    public boolean isBoolean(String s) {
        boolean isBool = s.equalsIgnoreCase("true")
                || s.equalsIgnoreCase("false");
        if (!isBool) {
            log.trace("Value {} is not boolean", s);
        }
        return isBool;
    }

    public boolean isNumeric(String s) {
        boolean numeric = StringUtils.isNumeric(s);
        if (!numeric) {
            log.trace("Value {} is not numeric", s);
        }
        return numeric;
    }

    public <T> T getFromAnnotatedField(Optional<Object> testInstance,
            Class<? extends Annotation> annotationClass,
            Class<T> capabilitiesClass) {
        if (capabilitiesClass == null) {
            throw new SeleniumJupiterException(
                    "The parameter capabilitiesClass must not be null");
        }
        return seekFieldAnnotatedWith(testInstance, annotationClass,
                capabilitiesClass).orElse(null);
    }

    public Optional<Object> seekFieldAnnotatedWith(
            Optional<Object> testInstance,
            Class<? extends Annotation> annotation) {
        return seekFieldAnnotatedWith(testInstance, annotation, null);
    }

    private static <T> Optional<T> seekFieldAnnotatedWith(
            Optional<Object> testInstance,
            Class<? extends Annotation> annotation, Class<T> annotatedType) {
        Optional<T> out = empty();
        try {
            if (testInstance.isPresent()) {
                Object object = testInstance.get();
                Class<? extends Object> clazz = object.getClass();
                out = getField(annotation, annotatedType, clazz, object);
                if (!out.isPresent()) {
                    // If annotation not present in class, look for it in the
                    // parent(s)
                    Class<?> superclass;
                    while ((superclass = clazz
                            .getSuperclass()) != Object.class) {
                        Optional<T> field = getField(annotation, annotatedType,
                                superclass, object);
                        out = field;
                        if (out.isPresent()) {
                            break;
                        }
                        clazz = clazz.getSuperclass();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Exception seeking field in {} annotated with {}",
                    annotatedType, annotation, e);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getField(
            Class<? extends Annotation> annotation, Class<T> annotatedType,
            Class<? extends Object> clazz, Object object)
            throws IllegalAccessException {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(annotation) && (annotatedType == null
                    || annotatedType.isAssignableFrom(field.getType()))) {
                field.setAccessible(true);
                if (annotatedType != null) {
                    return ofNullable(annotatedType.cast(field.get(object)));
                }
                return (Optional<T>) ofNullable(field.get(object));
            }
        }
        return empty();
    }

    public boolean getOpera(Parameter parameter) {
        return parameter.getAnnotation(Opera.class) != null;
    }

    public Binary getBinary(Parameter parameter) {
        return parameter.getAnnotation(Binary.class);
    }

    public Optional<DockerBrowser> getDocker(Parameter parameter) {
        Optional<DockerBrowser> out = empty();
        DockerBrowser dockerBrowser = parameter
                .getAnnotation(DockerBrowser.class);
        if (dockerBrowser != null) {
            out = of(dockerBrowser);
        }
        return out;
    }

    public Optional<Watch> getWatch(Parameter parameter) {
        Optional<Watch> out = empty();
        Watch watch = parameter.getAnnotation(Watch.class);
        if (watch != null) {
            out = of(watch);
        }
        return out;
    }

    public Optional<List<Object>> getKeyValue(String keyValue) {
        StringTokenizer st = new StringTokenizer(keyValue, "=");
        if (st.countTokens() != 2) {
            log.warn("Invalid format in {} (expected key=value)", keyValue);
            return empty();
        }
        String key = st.nextToken();
        String value = st.nextToken();
        Object returnedValue = value;
        if (isBoolean(value)) {
            returnedValue = Boolean.valueOf(value);
        } else if (isNumeric(value)) {
            returnedValue = Integer.valueOf(value);
        }
        return of(asList(key, returnedValue));
    }

}
