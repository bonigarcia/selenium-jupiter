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
package io.github.bonigarcia;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
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
                Optional<List<String>> keyValue = getKeyValue(capability);
                if (keyValue.isPresent()) {
                    ((DesiredCapabilities) capabilities).setCapability(
                            keyValue.get().get(0), keyValue.get().get(1));
                }
            }
            out = Optional.of(capabilities);
        } else {
            // If not, search DriverCapabilities in any field
            Optional<Object> annotatedField = seekFieldAnnotatedWith(
                    testInstance, DriverCapabilities.class);
            if (annotatedField.isPresent()) {
                capabilities = (Capabilities) annotatedField.get();
                out = Optional.of(capabilities);
            }
        }
        return out;
    }

    public Optional<URL> getUrl(Parameter parameter,
            Optional<Object> testInstance)
            throws MalformedURLException, IllegalAccessException {
        Optional<URL> out = empty();
        String urlValue = null;
        DriverUrl driverUrl = parameter.getAnnotation(DriverUrl.class);
        if (driverUrl != null) {
            // Search first DriverUrl annotation in parameter
            urlValue = driverUrl.value();
            out = Optional.of(new URL(urlValue));
        } else {
            // If not, search DriverUrl in any field
            Optional<Object> annotatedField = seekFieldAnnotatedWith(
                    testInstance, DriverUrl.class);
            if (annotatedField.isPresent()) {
                urlValue = (String) annotatedField.get();
                out = Optional.of(new URL(urlValue));
            }
        }
        return out;
    }

    public boolean isBoolean(String s) {
        boolean isBool = s.equalsIgnoreCase("true")
                || s.equalsIgnoreCase("false");
        if (!isBool) {
            log.warn("Value {} is not boolean", s);
        }
        return isBool;
    }

    public boolean isNumeric(String s) {
        boolean numeric = StringUtils.isNumeric(s);
        if (!numeric) {
            log.warn("Value {} is not numeric", s);
        }
        return numeric;
    }

    public Object getOptionsFromAnnotatedField(Optional<Object> testInstance,
            Class<Options> annotationClass) throws IllegalAccessException {
        Object out = null;
        Optional<Object> annotatedField = seekFieldAnnotatedWith(testInstance,
                annotationClass);
        if (annotatedField.isPresent()) {
            out = annotatedField.get();
        }
        return out;
    }

    public Optional<Object> seekFieldAnnotatedWith(
            Optional<Object> testInstance,
            Class<? extends Annotation> annotation)
            throws IllegalAccessException {
        Optional<Object> out = empty();
        if (testInstance.isPresent()) {
            Object object = testInstance.get();
            Class<? extends Object> clazz = object.getClass();
            out = getField(annotation, clazz, object);

            // If annotation not present in class, look for it in the parent(s)
            Class<?> superclass;
            while ((superclass = clazz.getSuperclass()) != Object.class) {
                out = getField(annotation, superclass, object);
                if (out.isPresent()) {
                    break;
                }
                clazz = clazz.getSuperclass();
            }
        }
        return out;
    }

    private Optional<Object> getField(Class<? extends Annotation> annotation,
            Class<? extends Object> clazz, Object object)
            throws IllegalAccessException {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(annotation)) {
                field.setAccessible(true);
                return Optional.of(field.get(object));
            }
        }
        return empty();
    }

    public Optional<DockerBrowser> getDocker(Parameter parameter) {
        Optional<DockerBrowser> out = empty();
        DockerBrowser dockerBrowser = parameter
                .getAnnotation(DockerBrowser.class);
        if (dockerBrowser != null) {
            out = Optional.of(dockerBrowser);
        }
        return out;
    }

    public Optional<List<String>> getKeyValue(String keyValue) {
        StringTokenizer st = new StringTokenizer(keyValue, "=");
        if (st.countTokens() != 2) {
            log.warn("Invalid format in {} (expected key=value)", keyValue);
            return empty();
        }
        return Optional.of(asList(st.nextToken(), st.nextToken()));
    }

}
