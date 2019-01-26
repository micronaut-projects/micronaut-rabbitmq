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

package io.micronaut.configuration.rabbitmq.bind;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import javax.annotation.concurrent.Immutable;

/**
 * Stores the state of a RabbitMQ message.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Immutable
public class RabbitMessageState {

    private final Envelope envelope;
    private final AMQP.BasicProperties properties;
    private final byte[] body;

    /**
     * Default constructor.
     *
     * @param envelope The envelope
     * @param properties The properties
     * @param body The body
     */
    public RabbitMessageState(Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        this.envelope = envelope;
        this.properties = properties;
        this.body = body;
    }

    /**
     * @return The body
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * @return The properties
     */
    public AMQP.BasicProperties getProperties() {
        return properties;
    }

    /**
     * @return The envelope
     */
    public Envelope getEnvelope() {
        return envelope;
    }
}
