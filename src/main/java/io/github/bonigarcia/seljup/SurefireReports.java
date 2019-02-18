/*
 * (C) Copyright 2018 Boni Garcia (http://bonigarcia.github.io/)
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
import static java.util.Arrays.stream;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

/**
 * Logic for surefire-reports output folder.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 2.1.0
 */
public class SurefireReports {

    static final Logger log = getLogger(lookup().lookupClass());

    private SurefireReports() {
        throw new IllegalStateException("Utility class");
    }

    public static String getOutputFolder(ExtensionContext context,
            String outputFolder) {
        if (context == null) {
            return "";
        }
        Optional<Method> testMethod = context.getTestMethod();
        Optional<Class<?>> testInstance = context.getTestClass();
        if (testMethod.isPresent() && testInstance.isPresent()) {
            if (outputFolder.equalsIgnoreCase("surefire-reports")) {
                outputFolder = getSurefireOutputFolder(testMethod.get(),
                        testInstance.get());
            } else if (outputFolder.isEmpty()) {
                outputFolder = ".";
            }
        }

        log.trace("Output folder {}", outputFolder);
        File outputFolderFile = new File(outputFolder);
        if (!outputFolderFile.exists()) {
            outputFolderFile.mkdirs();
        }
        return outputFolder;
    }

    private static String getSurefireOutputFolder(Method testMethod,
            Class<?> testInstance) {
        Annotation[] annotations = testMethod.getAnnotations();
        StringBuilder stringBuilder = new StringBuilder(
                "./target/surefire-reports/");

        boolean isTestTemplate = stream(annotations)
                .map(Annotation::annotationType)
                .anyMatch(a -> a == TestTemplate.class);
        log.trace("Is test template? {}", isTestTemplate);

        if (isTestTemplate) {
            stringBuilder.append(testMethod.getName());
            stringBuilder.append("(");

            Class<?>[] parameterTypes = testMethod.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i != 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(parameterTypes[i].getSimpleName());
            }
            stringBuilder.append(")/");

        } else {
            stringBuilder.append(testInstance.getName());
        }
        return stringBuilder.toString();
    }

}
