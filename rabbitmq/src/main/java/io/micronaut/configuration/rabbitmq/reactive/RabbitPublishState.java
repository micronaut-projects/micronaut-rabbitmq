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

package io.micronaut.configuration.rabbitmq.reactive;

import com.rabbitmq.client.AMQP;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Stores the state of a RabbitMQ message to be published.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Immutable
public class RabbitPublishState {

    private final String exchange;
    private final String routingKey;
    private final AMQP.BasicProperties properties;
    private final byte[] body;

    /**
     * Default constructor.
     *
     * @param exchange The exchange
     * @param routingKey The routing key
     * @param properties The properties
     * @param body The body
     */
    public RabbitPublishState(String exchange, String routingKey, AMQP.BasicProperties properties, @Nullable byte[] body) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.properties = properties;
        this.body = body;
    }

    /**
     * @return The exchange to publish the message to
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * @return The routing key
     */
    public String getRoutingKey() {
        return routingKey;
    }

    /**
     * @return The properties
     */
    public AMQP.BasicProperties getProperties() {
        return properties;
    }

    /**
     * @return The message body
     */
    @Nullable
    public byte[] getBody() {
        return body;
    }
}
