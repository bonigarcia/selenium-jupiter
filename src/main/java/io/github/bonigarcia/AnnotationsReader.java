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
import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

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

    static AnnotationsReader instance;

    public static synchronized AnnotationsReader getInstance() {
        if (instance == null) {
            instance = new AnnotationsReader();
        }
        return instance;
    }

    public Optional<Capabilities> getCapabilities(Parameter parameter,
            Optional<Object> testInstance) {
        Optional<Capabilities> out = empty();
        DriverCapabilities driverCapabilities = parameter
                .getAnnotation(DriverCapabilities.class);

        Capabilities capabilities = null;
        if (driverCapabilities != null) {
            // Search first DriverCapabilities annotation in parameter
            capabilities = new DesiredCapabilities();
            for (Capability capability : driverCapabilities.capability()) {
                ((DesiredCapabilities) capabilities)
                        .setCapability(capability.name(), capability.value());
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
            Optional<Object> testInstance) throws MalformedURLException {
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
            Class<DriverOptions> annotationClass) {
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
            Class<? extends Annotation> annotation) {
        Optional<Object> out = empty();
        if (testInstance.isPresent()) {
            Object object = testInstance.get();
            Field[] declaredFields = object.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(annotation)) {
                    try {
                        field.setAccessible(true);
                        out = Optional.of(field.get(object));
                    } catch (Exception e) {
                        log.warn(
                                "Exception searching annotation {} in test instance {}",
                                annotation, testInstance, e);
                    }
                }
            }
        }
        return out;
    }

}
