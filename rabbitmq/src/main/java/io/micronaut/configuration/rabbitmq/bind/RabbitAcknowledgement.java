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
package io.micronaut.configuration.rabbitmq.bind;

import io.micronaut.messaging.Acknowledgement;
import io.micronaut.messaging.exceptions.MessageAcknowledgementException;

/**
 * A contract for acknowledging or rejecting RabbitMQ messages.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public interface RabbitAcknowledgement extends Acknowledgement {

    @Override
    default void ack() throws MessageAcknowledgementException {
        ack(false);
    }

    @Override
    default void nack() throws MessageAcknowledgementException {
        nack(false, false);
    }

    /**
     * Acknowledges this message.
     *
     * @param multiple If true, also acknowledge previous messages
     * @throws MessageAcknowledgementException If an error occurred
     */
    void ack(boolean multiple) throws MessageAcknowledgementException;

    /**
     * Rejects this message.
     *
     * @param multiple If true, also reject previous messages
     * @param reQueue Re-queue the message to be consumed again
     * @throws MessageAcknowledgementException If an error occurred
     */
    void nack(boolean multiple, boolean reQueue) throws MessageAcknowledgementException;
}
