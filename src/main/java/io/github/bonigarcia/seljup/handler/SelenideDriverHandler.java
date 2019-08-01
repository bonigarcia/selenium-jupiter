/*
 * (C) Copyright 2019 Boni Garcia (http://bonigarcia.github.io/)
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

import org.junit.jupiter.api.extension.ExtensionContext;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;

import io.github.bonigarcia.seljup.AnnotationsReader;
import io.github.bonigarcia.seljup.config.Config;

/**
 * Resolver for SelenideDriver.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 3.2.0
 */
public class SelenideDriverHandler extends DriverHandler {

    public SelenideDriverHandler(Config config,
            AnnotationsReader annotationsReader) {
        super(config, annotationsReader);
    }

    public SelenideDriverHandler(Parameter parameter, ExtensionContext context,
            Config config, AnnotationsReader annotationsReader) {
        super(parameter, context, config, annotationsReader);
    }

    @Override
    public void resolve() {
        try {
            object = new SelenideDriver(new SelenideConfig());
        } catch (Exception e) {
            handleException(e);
        }
    }

}
