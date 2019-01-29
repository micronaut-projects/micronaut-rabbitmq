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

import io.micronaut.configuration.rabbitmq.serialization.RabbitMessageSerDesRegistry;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;

import javax.inject.Singleton;

/**
 * The default binder for binding an argument from the {@link RabbitMessageState}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitDefaultBinder implements RabbitArgumentBinder<Object> {

    private final RabbitPropertyBinder propertyBinder;
    private final RabbitMessageSerDesRegistry serDesRegistry;

    /**
     * Default constructor.
     *
     * @param propertyBinder The property binder
     * @param serDesRegistry The registry to find a serializer
     */
    public RabbitDefaultBinder(RabbitPropertyBinder propertyBinder,
                               RabbitMessageSerDesRegistry serDesRegistry) {
        this.propertyBinder = propertyBinder;
        this.serDesRegistry = serDesRegistry;
    }

    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitMessageState source) {
        if (propertyBinder.supports(context)) {
            return propertyBinder.bind(context, source);
        } else {
            byte[] value = source.getBody();
            Class<Object> bodyType = context.getArgument().getType();

            return () -> serDesRegistry.findSerdes(bodyType)
                    .map(serDes -> serDes.deserialize(value, bodyType));
        }
    }
}
