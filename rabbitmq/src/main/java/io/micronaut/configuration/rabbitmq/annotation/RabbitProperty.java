/*
 * Copyright 2017-2018 original authors
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

import io.micronaut.core.bind.annotation.Bindable;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to set properties while publishing or bind to properties while consuming.
 *
 * For example while producing messages, the annotation can be set at the method
 * or class level to provide static data. It can be applied at the parameter level
 * to set the property differently per execution.
 *
 * While consuming the annotation can be applied to a parameter to bind the
 * property to the argument.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.METHOD})
@Repeatable(value = RabbitProperties.class)
@Bindable
public @interface RabbitProperty {

    /**
     * If used as a bound parameter, this is the property name. If used on a class
     * level this is value and not the property name.
     *
     * @return The name of the property, otherwise it is inferred from the {@link #name()}
     */
    String value() default "";

    /**
     * Never used if applied to a parameter. Supplies the property name if used on
     * a class or method.
     *
     * @return The name of property
     */
    String name() default "";
}
