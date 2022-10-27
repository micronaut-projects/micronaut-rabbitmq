/*
 * Copyright 2017-2021 original authors
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
import io.micronaut.context.annotation.Executable;
import io.micronaut.messaging.annotation.MessageMapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify which queue messages should be consumed
 * from. The queue must already exist in the broker.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Executable
@Inherited
public @interface Queue {

    /**
     * @return The queue to subscribe to.
     */
    @AliasFor(annotation = MessageMapping.class, member = "value")
    String value();

    /**
     * Only applies to consumer methods that do not inject an
     * {@link io.micronaut.messaging.Acknowledgement} instance.
     *
     * @return Whether nacks should re-queue the message.
     */
    boolean reQueue() default false;

    /**
     * @return Whether the consumer is exclusive to the queue
     */
    boolean exclusive() default false;

    /**
     * @return Whether the consumer should consider messages acknowledged once delivered
     */
    boolean autoAcknowledgment() default false;

    /**
     * @return The unacknowledged message limit
     */
    int prefetch() default 0;

    /**
     * @return The number of consumers used to consumer from a queue concurrently
     */
    int numberOfConsumers() default 1;

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
