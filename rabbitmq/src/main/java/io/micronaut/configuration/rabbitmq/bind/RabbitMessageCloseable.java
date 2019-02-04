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

import com.rabbitmq.client.Channel;
import io.micronaut.messaging.exceptions.MessageAcknowledgementException;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Responsible for acknowledging or rejecting a message. Only
 * applies if the consuming method does not have an {@link io.micronaut.messaging.Acknowledgement}
 * argument.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public class RabbitMessageCloseable implements AutoCloseable {

    private final Channel channel;
    private final long deliveryTag;
    private final boolean multiple;
    private final boolean reQueue;
    private Boolean acknowledge = null;

    /**
     * Default constructor.
     *
     * @param messageState The message state
     * @param multiple Whether multiple messages should be responded to
     * @param reQueue Whether rejections should cause the messages to be re-queued
     */
    public RabbitMessageCloseable(RabbitMessageState messageState,
                           boolean multiple,
                           boolean reQueue) {
        this.channel = messageState.getChannel();
        this.deliveryTag = messageState.getEnvelope().getDeliveryTag();
        this.multiple = multiple;
        this.reQueue = reQueue;
    }

    @Override
    public void close() throws MessageAcknowledgementException {
        if (acknowledge != null) {
            if (acknowledge) {
                try {
                    channel.basicAck(deliveryTag, multiple);
                } catch (IOException e) {
                    throw new MessageAcknowledgementException("An error occurred acknowledging a message", e);
                }
            } else {
                try {
                    channel.basicNack(deliveryTag, multiple, reQueue);
                } catch (IOException e) {
                    throw new MessageAcknowledgementException("An error occurred rejecting a message", e);
                }
            }
        }
    }

    /**
     * Sets whether the message should be acknowledged.
     *
     * Set to true if the message should be acknowledged.
     * Set to false if the message should be rejected.
     * Set to null if the message should not be acknowledged or rejected.
     *
     * @param acknowledge The acknowledge parameter.
     */
    public void setAcknowledge(@Nullable Boolean acknowledge) {
        this.acknowledge = acknowledge;
    }
}
