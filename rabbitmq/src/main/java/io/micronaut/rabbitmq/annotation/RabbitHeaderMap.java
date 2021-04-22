/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.rabbitmq.annotation;

import io.micronaut.core.bind.annotation.Bindable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to set message headers as a method parameter while publishing.
 *
 * For example while producing messages, the annotation can be set at the method
 * parameter level to provide a map of key values of headers.
 *
 * @see com.rabbitmq.client.BasicProperties
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER})
@Bindable
public @interface RabbitHeaderMap {

    /**
     * Map of Headers
     *
     * @return The map of Headers
     */
    Map<String, String> headerMap() default null;
}
