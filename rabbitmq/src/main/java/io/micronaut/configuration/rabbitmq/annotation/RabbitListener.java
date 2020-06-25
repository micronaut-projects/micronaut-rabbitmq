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
package io.micronaut.configuration.rabbitmq.annotation;

import io.micronaut.context.annotation.AliasFor;
import io.micronaut.messaging.annotation.MessageListener;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class level annotation to indicate that a bean will be consumers of messages
 * from RabbitMQ.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE})
@MessageListener
public @interface RabbitListener {

    /**
     * @see RabbitConnection#connection()
     * @return The connection to use
     */
    @AliasFor(annotation = RabbitConnection.class, member = "connection")
    String connection() default "";

    /**
     * @see RabbitConnection#executor()
     * @return The executor to use
     */
    @AliasFor(annotation = RabbitConnection.class, member = "executor")
    String executor() default "";
}
