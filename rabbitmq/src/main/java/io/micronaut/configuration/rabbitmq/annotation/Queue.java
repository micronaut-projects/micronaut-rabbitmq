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

import java.lang.annotation.*;

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
public @interface Queue {

    /**
     * @return The queue to subscribe to.
     */
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

}
