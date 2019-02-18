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
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Binds an argument of type {@link Channel} from the {@link RabbitConsumerState}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitChannelBinder implements RabbitTypeArgumentBinder<Channel> {

    @Override
    public Argument<Channel> argumentType() {
        return Argument.of(Channel.class);
    }

    @Override
    public BindingResult<Channel> bind(ArgumentConversionContext<Channel> context, RabbitConsumerState messageState) {
        return () -> Optional.of(messageState.getChannel());
    }
}
