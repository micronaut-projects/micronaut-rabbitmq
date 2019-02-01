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

import io.micronaut.configuration.rabbitmq.serdes.RabbitMessageSerDesRegistry;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.messaging.annotation.Body;

import javax.inject.Singleton;

/**
 * Binds an argument of with the {@link Body} annotation from the {@link RabbitMessageState}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitBodyBinder implements RabbitAnnotatedArgumentBinder<Body> {

    private final ConversionService conversionService;
    private final RabbitMessageSerDesRegistry serDesRegistry;

    /**
     * Default constructor.
     *
     * @param conversionService The conversion service to convert the body
     * @param serDesRegistry The registry to get a deserializer
     */
    public RabbitBodyBinder(ConversionService conversionService,
                            RabbitMessageSerDesRegistry serDesRegistry) {
        this.conversionService = conversionService;
        this.serDesRegistry = serDesRegistry;
    }

    @Override
    public Class<Body> getAnnotationType() {
        return Body.class;
    }

    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitMessageState messageState) {
        Class<Object> bodyType = context.getArgument().getType();

        return () -> serDesRegistry.findSerdes(bodyType)
                .map(serDes -> serDes.deserialize(messageState.getBody(), bodyType, messageState));
    }
}
