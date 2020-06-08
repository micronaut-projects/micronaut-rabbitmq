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

import io.micronaut.context.annotation.AliasFor;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.messaging.annotation.MessageMapping;

import java.lang.annotation.*;

/**
 * Used to specify which binding (routing key) messages should be
 * sent to. Can be applied to a parameter for a dynamic value per
 * execution.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Bindable
public @interface Binding {

    /**
     * Should always be supplied when the annotation is applied to a method.
     *
     * @return The binding (routing key) to publish messages to.
     */
    @AliasFor(annotation = MessageMapping.class, member = "value")
    String value() default "";

    /**
     * @see RabbitConnection#connection()
     * @return The connection to use
     */
    @AliasFor(annotation = RabbitConnection.class, member = "connection")
    String connection() default "";
}
