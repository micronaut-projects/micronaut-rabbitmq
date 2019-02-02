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

import io.micronaut.configuration.rabbitmq.bind.RabbitMessageState;
import io.micronaut.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * The default ExceptionHandler used when a {@link io.micronaut.configuration.rabbitmq.annotation.RabbitListener}
 * fails to process a RabbitMQ message. By default just logs the error.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
@Primary
public class DefaultRabbitListenerExceptionHandler implements RabbitListenerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRabbitListenerExceptionHandler.class);

    @Override
    public void handle(RabbitListenerException exception) {
        if (LOG.isErrorEnabled()) {
            Optional<RabbitMessageState> messageState = exception.getMessageState();
            if (messageState.isPresent()) {
                LOG.error("Error processing a message for RabbitMQ consumer [" + exception.getListener() + "]", exception);
            } else {
                LOG.error("RabbitMQ consumer [" + exception.getListener() + "] produced an error", exception);
            }
        }
    }
}
