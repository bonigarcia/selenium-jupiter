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
package io.github.bonigarcia.seljup.handler;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;

import io.github.bonigarcia.seljup.config.Config;

/**
 * Resolver for other drivers.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 1.2.0
 */
public class OtherDriverHandler extends DriverHandler {

    private Class<?> type;

    public OtherDriverHandler(Parameter parameter, ExtensionContext context,
            Config config) {
        super(parameter, context, config);
    }

    public OtherDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, Class<?> type) {
        super(parameter, context, config);
        this.type = type;
    }

    @Override
    public void resolve() {
        try {
            Optional<Object> testInstance = context.getTestInstance();
            if (type == null) {
                type = parameter.getType();
            }
            Optional<Capabilities> capabilities = annotationsReader
                    .getCapabilities(parameter, testInstance);
            if (capabilities.isPresent()) {
                object = type.getDeclaredConstructor(Capabilities.class)
                        .newInstance(capabilities.get());
            } else {
                object = type.newInstance();
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

}
