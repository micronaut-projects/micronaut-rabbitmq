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

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.messaging.Acknowledgement;
import io.micronaut.messaging.exceptions.MessageAcknowledgementException;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Binds an argument of type {@link Acknowledgement} from the {@link RabbitMessageState}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitAcknowledgementBinder implements RabbitTypeArgumentBinder<Acknowledgement> {

    @Override
    public Argument<Acknowledgement> argumentType() {
        return Argument.of(Acknowledgement.class);
    }

    @Override
    public BindingResult<Acknowledgement> bind(ArgumentConversionContext<Acknowledgement> context, RabbitMessageState source) {
        Acknowledgement acknowledgement = new RabbitAcknowledgement() {
            @Override
            public void ack(boolean multiple) throws MessageAcknowledgementException {
                ackNack(true, multiple, false);
            }

            @Override
            public void nack(boolean multiple, boolean reQueue) throws MessageAcknowledgementException {
                ackNack(false, multiple, reQueue);
            }

            private void ackNack(boolean ack, boolean multiple, boolean requeue)  throws MessageAcknowledgementException {
                new RabbitMessageCloseable(source, multiple, requeue).withAcknowledge(ack).close();
            }
        };
        return () -> Optional.of(acknowledgement);
    }
}
