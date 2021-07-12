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

import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.AliasFor;
import io.micronaut.context.annotation.Type;
import io.micronaut.rabbitmq.intercept.RabbitMQIntroductionAdvice;
import io.micronaut.retry.annotation.Recoverable;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An introduction advice that automatically implements interfaces and abstract classes and publishes RabbitMQ messages.
 *
 * @author James Kleeh
 * @since 1.1.0
 * @see RabbitMQIntroductionAdvice
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE})
@Scope
@Introduction
@Type(RabbitMQIntroductionAdvice.class)
@Recoverable
@Singleton
@Inherited
public @interface RabbitClient {

    /**
     * @return The exchange to publish messages to.
     */
    String value() default "";

    /**
     * @see RabbitConnection#connection()
     * @return The connection to use
     */
    @AliasFor(annotation = RabbitConnection.class, member = "connection")
    String connection() default "";

}
