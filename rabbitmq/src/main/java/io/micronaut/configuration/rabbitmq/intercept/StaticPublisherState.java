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

package io.micronaut.configuration.rabbitmq.intercept;

import io.micronaut.configuration.rabbitmq.serdes.RabbitMessageSerDes;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Stores the static state for publishing messages with {@link io.micronaut.configuration.rabbitmq.annotation.RabbitClient}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Internal
class StaticPublisherState {

    private final String exchange;
    private final String routingKey;
    private final Argument bodyArgument;
    private final Map<String, Object> headers;
    private final Map<String, String> properties;
    private final boolean reactive;
    private final ReturnType<?> returnType;
    private final Argument<?> dataType;
    private final RabbitMessageSerDes<?> serDes;

    /**
     * Default constructor.
     *
     * @param exchange The exchange to publish to
     * @param routingKey The routing key
     * @param bodyArgument The argument representing the body
     * @param headers The static headers
     * @param properties The static properties
     * @param returnType The return type of the method
     * @param serDes The body serializer
     */
    StaticPublisherState(String exchange,
                         @Nullable String routingKey,
                         Argument bodyArgument,
                         Map<String, Object> headers,
                         Map<String, String> properties,
                         ReturnType<?> returnType,
                         RabbitMessageSerDes<?> serDes) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.bodyArgument = bodyArgument;
        this.headers = headers;
        this.properties = properties;
        Class<?> javaReturnType = returnType.getType();
        this.reactive = Publishers.isConvertibleToPublisher(javaReturnType);
        if (this.reactive) {
            this.dataType = returnType.getFirstTypeVariable()
                    .orElse(Argument.VOID);
        } else {
            this.dataType = returnType.asArgument();
        }
        this.returnType = returnType;
        this.serDes = serDes;
    }

    /**
     * @return The exchange
     */
    String getExchange() {
        return exchange;
    }

    /**
     * @return The optional routing key
     */
    Optional<String> getRoutingKey() {
        return Optional.ofNullable(routingKey);
    }

    /**
     * @return The body argument
     */
    Argument getBodyArgument() {
        return bodyArgument;
    }

    /**
     * @return The headers
     */
    Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * @return The properties
     */
    Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @return True if the method returns a reactive type
     */
    boolean isReactive() {
        return reactive;
    }

    /**
     * @return The serializer
     */
    RabbitMessageSerDes<Object> getSerDes() {
        return (RabbitMessageSerDes) serDes;
    }

    /**
     * @return The type of data being requested
     */
    Argument<?> getDataType() {
        return dataType;
    }

    /**
     * @return The return type
     */
    ReturnType<?> getReturnType() {
        return returnType;
    }
}
