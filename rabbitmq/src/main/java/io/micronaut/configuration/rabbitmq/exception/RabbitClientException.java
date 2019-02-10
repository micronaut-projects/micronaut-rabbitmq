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

package io.micronaut.configuration.rabbitmq.exception;

import io.micronaut.configuration.rabbitmq.reactive.RabbitPublishState;
import io.micronaut.messaging.exceptions.MessagingClientException;

import java.util.List;
import java.util.Optional;

/**
 * Exception thrown when an error occurs publishing a RabbitMQ message.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public class RabbitClientException extends MessagingClientException {

    private final List<RabbitPublishState> failures;

    /**
     * Creates a new exception.
     *
     * @param message The message
     */
    public RabbitClientException(String message) {
        super(message);
        this.failures = null;
    }

    /**
     * Creates a new exception.
     *
     * @param message The message
     * @param failures The messages that failed to send
     */
    public RabbitClientException(String message, List<RabbitPublishState> failures) {
        super(message);
        this.failures = failures;
    }

    /**
     * Creates a new exception.
     *
     * @param message The message
     * @param cause The cause
     */
    public RabbitClientException(String message, Throwable cause) {
        this(message, cause, null);
    }


    /**
     * Creates a new exception.
     *
     * @param message The message
     * @param cause The cause
     * @param failures The messages that failed to send
     */
    public RabbitClientException(String message, Throwable cause, List<RabbitPublishState> failures) {
        super(message, cause);
        this.failures = failures;
    }

    /**
     * @return An optional list of messages that failed to send
     */
    public Optional<List<RabbitPublishState>> getFailures() {
        return Optional.ofNullable(failures);
    }
}
