/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.rabbitmq.exception;

import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.messaging.exceptions.MessageListenerException;
import io.micronaut.rabbitmq.annotation.RabbitListener;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Exception thrown when an error occurs processing a RabbitMQ message via a {@link RabbitListener}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public class RabbitListenerException extends MessageListenerException {

    private final Object listener;
    private final RabbitConsumerState messageState;

    /**
     * Creates a new exception.
     *
     * @param message The message
     * @param listener The listener
     * @param messageState The message
     */
    public RabbitListenerException(String message, Object listener, @Nullable RabbitConsumerState messageState) {
        super(message);
        this.listener = listener;
        this.messageState = messageState;
    }

    /**
     * Creates a new exception.
     *
     * @param message The message
     * @param cause The cause
     * @param listener The listener
     * @param messageState The message
     */
    public RabbitListenerException(String message, Throwable cause, Object listener, @Nullable RabbitConsumerState messageState) {
        super(message, cause);
        this.listener = listener;
        this.messageState = messageState;
    }
    
    /**
     * Creates a new exception.
     *
     * @param cause The cause
     * @param listener The listener
     * @param messageState The message
     */
    public RabbitListenerException(Throwable cause, Object listener, @Nullable RabbitConsumerState messageState) {
        super(cause.getMessage(), cause);
        this.listener = listener;
        this.messageState = messageState;
    }

    /**
     * @return The bean that is the kafka listener
     */
    public Object getListener() {
        return listener;
    }

    /**
     * @return The consumer that produced the error
     */
    public Optional<RabbitConsumerState> getMessageState() {
        return Optional.ofNullable(messageState);
    }
}
