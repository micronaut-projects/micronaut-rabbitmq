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
import io.micronaut.core.util.StringUtils;

import java.lang.annotation.*;

/**
 * Used to specify how the server should react if the message cannot be routed to a queue.
 *
 * <p>If this flag is {@code true}, the server will return an unroutable message with a Return method.
 * If this flag is {@code false}, the server silently drops the message.</p>
 *
 * <p>Can be applied to a parameter for a dynamic value per execution.</p>
 *
 * @since 4.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Bindable
@Inherited
public @interface Mandatory {

    /**
     * @return Whether the "mandatory" flag should be set or not.
     */
    String value() default StringUtils.TRUE;
}
