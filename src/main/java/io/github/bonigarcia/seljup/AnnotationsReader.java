/*
 * (C) Copyright 2017 Boni Garcia (http://bonigarcia.github.io/)
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
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
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
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.0.0
 */
public class AnnotationsReader {

    final Logger log = getLogger(lookup().lookupClass());

    public Optional<Capabilities> getCapabilities(Parameter parameter,
            Optional<Object> testInstance) throws IllegalAccessException {
        Optional<Capabilities> out = empty();
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
        return out;
    }

    public Optional<URL> getUrl(Parameter parameter,
            Optional<Object> testInstance, String seleniumServerUrl)
            throws MalformedURLException, IllegalAccessException {
        Optional<URL> out = empty();

        if (seleniumServerUrl != null && !seleniumServerUrl.isEmpty()) {
            out = of(new URL(seleniumServerUrl));
        } else {
            String urlValue = null;
            DriverUrl driverUrl = parameter.getAnnotation(DriverUrl.class);
            if (driverUrl != null) {
                // Search first DriverUrl annotation in parameter
                urlValue = driverUrl.value();
                out = of(new URL(urlValue));
            } else {
                // If not, search DriverUrl in any field
                Optional<Object> annotatedField = seekFieldAnnotatedWith(
                        testInstance, DriverUrl.class);
                if (annotatedField.isPresent()) {
                    urlValue = (String) annotatedField.get();
                    out = of(new URL(urlValue));
                }
            }
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

    public Object getOptionsFromAnnotatedField(Optional<Object> testInstance,
            Class<Options> annotationClass) throws IllegalAccessException {
        Object out = null;
        Optional<Object> annotatedField = seekFieldAnnotatedWith(testInstance,
                annotationClass, null);
        if (annotatedField.isPresent()) {
            out = annotatedField.get();
        }
        return out;
    }

    public <T extends Capabilities> T getOptionsFromAnnotatedField(
            Optional<Object> testInstance, Class<Options> annotationClass,
            Class<T> capabilitiesClass) throws IllegalAccessException {
        if (capabilitiesClass == null) {
            throw new SeleniumJupiterException(
                    "The parameter capabilitiesClass must not be null.");
        }
        return seekFieldAnnotatedWith(testInstance, annotationClass,
                capabilitiesClass).orElse(null);
    }

    public Optional<Object> seekFieldAnnotatedWith(
            Optional<Object> testInstance,
            Class<? extends Annotation> annotation)
            throws IllegalAccessException {
        return seekFieldAnnotatedWith(testInstance, annotation, null);
    }

    private static <T> Optional<T> seekFieldAnnotatedWith(
            Optional<Object> testInstance,
            Class<? extends Annotation> annotation, Class<T> annotatedType)
            throws IllegalAccessException {
        Optional<T> out = empty();
        if (testInstance.isPresent()) {
            Object object = testInstance.get();
            Class<? extends Object> clazz = object.getClass();
            out = getField(annotation, annotatedType, clazz, object);
            if (!out.isPresent()) {
                // If annotation not present in class, look for it in the
                // parent(s)
                Class<?> superclass;
                while ((superclass = clazz.getSuperclass()) != Object.class) {
                    out = getField(annotation, annotatedType, superclass,
                            object);
                    if (out.isPresent()) {
                        break;
                    }
                    clazz = clazz.getSuperclass();
                }
            }
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
                    || annotatedType == field.getType())) {
                field.setAccessible(true);
                if (annotatedType != null) {
                    return of(annotatedType.cast(field.get(object)));
                }
                return (Optional<T>) of(field.get(object));
            }
        }
        return empty();
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
            returnedValue = new Boolean(value);
        } else if (isNumeric(value)) {
            returnedValue = new Integer(value);
        }
        return of(asList(key, returnedValue));
    }

}
