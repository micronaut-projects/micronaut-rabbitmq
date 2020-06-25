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

import com.rabbitmq.client.BasicProperties;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Binds an argument of type {@link BasicProperties} from the {@link RabbitConsumerState}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitBasicPropertiesBinder implements RabbitTypeArgumentBinder<BasicProperties> {

    @Override
    public Argument<BasicProperties> argumentType() {
        return Argument.of(BasicProperties.class);
    }

    @Override
    public BindingResult<BasicProperties> bind(ArgumentConversionContext<BasicProperties> context, RabbitConsumerState messageState) {
        Optional<BasicProperties> properties = Optional.of(messageState.getProperties());
        return () -> properties;
    }
}
