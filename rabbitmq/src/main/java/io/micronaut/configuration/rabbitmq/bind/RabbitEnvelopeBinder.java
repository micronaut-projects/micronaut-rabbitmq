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
package io.micronaut.configuration.rabbitmq.bind;

import com.rabbitmq.client.Envelope;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Binds an argument of type {@link Envelope} from the {@link RabbitConsumerState}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitEnvelopeBinder implements RabbitTypeArgumentBinder<Envelope> {

    @Override
    public Argument<Envelope> argumentType() {
        return Argument.of(Envelope.class);
    }

    @Override
    public BindingResult<Envelope> bind(ArgumentConversionContext<Envelope> context, RabbitConsumerState messageState) {
        Optional<Envelope> envelope = Optional.of(messageState.getEnvelope());
        return () -> envelope;
    }
}
