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

package io.micronaut.configuration.rabbitmq.serdes;

import io.micronaut.configuration.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.configuration.rabbitmq.intercept.MutableBasicProperties;
import io.micronaut.core.order.Ordered;

import javax.annotation.Nullable;

/**
 * Responsible for serializing and deserializing RabbitMQ message bodies.
 *
 * @param <T> The type to be serialized/deserialized
 * @author James Kleeh
 * @since 1.1.0
 */
public interface RabbitMessageSerDes<T> extends Ordered {

    /**
     * Deserializes the message into the requested type.
     *
     * @param consumerState The message to deserialize
     * @param type The type to be returned
     * @return The deserialized body
     */
    T deserialize(RabbitConsumerState consumerState, Class<T> type);

    /**
     * Serializes the data into a byte[] to be published
     * to RabbitMQ.
     *
     * @param data The data to serialize
     * @param type The type to serialize
     * @param properties The properties of the message
     * @return The message body
     */
    byte[] serialize(@Nullable T data, Class<Object> type, MutableBasicProperties properties);

    /**
     * Determines if this serdes supports the given type.
     *
     * @param type The type
     * @return True if the type is supported
     */
    boolean supports(Class<T> type);
}
