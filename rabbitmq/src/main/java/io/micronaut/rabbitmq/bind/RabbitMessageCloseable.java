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

import com.rabbitmq.client.Channel;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.messaging.exceptions.MessageAcknowledgementException;

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
    private AcknowledgmentAction acknowledgmentAction = AcknowledgmentAction.NONE;

    /**
     * Default constructor.
     *
     * @param consumerState The message state
     * @param multiple Whether multiple messages should be responded to
     * @param reQueue Whether rejections should cause the messages to be re-queued
     */
    public RabbitMessageCloseable(RabbitConsumerState consumerState,
                           boolean multiple,
                           boolean reQueue) {
        this.channel = consumerState.getChannel();
        this.deliveryTag = consumerState.getEnvelope().getDeliveryTag();
        this.multiple = multiple;
        this.reQueue = reQueue;
    }

    @Override
    public void close() throws MessageAcknowledgementException {
        switch (acknowledgmentAction) {
            case NONE:
            default:
                break;
            case ACK:
                try {
                    channel.basicAck(deliveryTag, multiple);
                } catch (IOException e) {
                    throw new MessageAcknowledgementException("An error occurred acknowledging a message", e);
                }
                break;
            case NACK:
                try {
                    channel.basicNack(deliveryTag, multiple, reQueue);
                } catch (IOException e) {
                    throw new MessageAcknowledgementException("An error occurred rejecting a message", e);
                }
                break;
        }
    }

    /**
     * Builder style sets whether the message should be acknowledged.
     *
     * Set to true if the message should be acknowledged.
     * Set to false if the message should be rejected.
     * Set to null if the message should not be acknowledged or rejected.
     *
     * @deprecated as of 3.4.0
     *
     * @param acknowledge The acknowledge parameter.
     * @return The same instance
     */
    @Deprecated
    public RabbitMessageCloseable withAcknowledge(@Nullable Boolean acknowledge) {
        if (acknowledge == null) {
            acknowledgmentAction = AcknowledgmentAction.NONE;
        } else if (Boolean.TRUE.equals(acknowledge)) {
            acknowledgmentAction = AcknowledgmentAction.ACK;
        } else {
            acknowledgmentAction = AcknowledgmentAction.NACK;
        }
        return this;
    }

    /**
     * Builder style sets whether the message acknowledgment action should be addressed and message should be acknowledged or rejected,
     * or otherwise should acknowledgment action be skipped.
     *
     * Set to {@link AcknowledgmentAction#ACK} if the message should be acknowledged.
     * Set to {@link AcknowledgmentAction#NACK} if the message should be rejected.
     * Set to {@link AcknowledgmentAction#NONE} if the message should not be acknowledged or rejected.
     *
     * @since 3.4.0
     *
     * @param acknowledgmentAction The acknowledgment action parameter.
     * @return The same instance
     */
    public RabbitMessageCloseable withAcknowledgmentAction(@NonNull AcknowledgmentAction acknowledgmentAction) {
        this.acknowledgmentAction = acknowledgmentAction;
        return this;
    }
}
