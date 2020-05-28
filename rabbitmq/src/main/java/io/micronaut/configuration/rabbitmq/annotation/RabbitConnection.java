/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.rabbitmq.annotation;

import io.micronaut.configuration.rabbitmq.connect.SingleRabbitConnectionFactoryConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Stores options surrounding a RabbitMQ connection.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RabbitConnection {

    String DEFAULT_CONNECTION = SingleRabbitConnectionFactoryConfig.DEFAULT_NAME;

    /**
     * @return The connection to use
     */
    String connection() default DEFAULT_CONNECTION;

    /**
     * @return The executor service bean name qualifier to handle
     * the consumer method execution.
     */
    String executor() default "";
}
