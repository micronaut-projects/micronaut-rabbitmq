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
package io.micronaut.rabbitmq.bind;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import io.micronaut.core.annotation.NonNull;

/**
 * Stores the state of a RabbitMQ message to be consumed.
 *
 * This class should be treated as immutable.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public class RabbitConsumerState {

    private final Envelope envelope;
    private final AMQP.BasicProperties properties;
    private final byte[] body;
    private final Channel channel;

    /**
     * Default constructor.
     *
     * @param envelope The envelope
     * @param properties The properties
     * @param body The body
     * @param channel The channel that consumed the message
     */
    public RabbitConsumerState(Envelope envelope,
                              AMQP.BasicProperties properties,
                              byte[] body,
                              Channel channel) {
        this.envelope = envelope;
        this.properties = properties;
        this.body = body;
        this.channel = channel;
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
    @NonNull
    public AMQP.BasicProperties getProperties() {
        return properties;
    }

    /**
     * @return The envelope
     */
    @NonNull
    public Envelope getEnvelope() {
        return envelope;
    }

    /**
     * This channel is being used exclusively by the consumer that
     * consumed this message. Attempts to use this channel for any
     * purpose other than acknowledgement may result in errors because
     * channels are not thread safe.
     *
     * @return The channel
     */
    public Channel getChannel() {
        return channel;
    }
}
