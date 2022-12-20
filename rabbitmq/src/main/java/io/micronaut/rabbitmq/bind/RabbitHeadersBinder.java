/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.rabbitmq.bind;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.rabbitmq.annotation.RabbitHeaders;
import jakarta.inject.Singleton;

/**
 * Binds an argument of with the {@link RabbitHeaders} annotation from the {@link RabbitConsumerState}.
 *
 * @author James Kleeh
 * @since 2.5.0
 */
@Singleton
public class RabbitHeadersBinder implements RabbitAnnotatedArgumentBinder<RabbitHeaders> {

    private final ConversionService conversionService;

    /**
     * Default constructor.
     *
     * @param conversionService The conversion service to convert the body
     */
    public RabbitHeadersBinder(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Class<RabbitHeaders> getAnnotationType() {
        return RabbitHeaders.class;
    }

    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitConsumerState source) {
        return () -> conversionService.convert(source.getProperties().getHeaders(), context);
    }
}
