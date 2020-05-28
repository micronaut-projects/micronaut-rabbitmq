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
package io.micronaut.rabbitmq.bind;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.messaging.annotation.Header;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * Binds an argument of with the {@link Header} annotation from the {@link RabbitConsumerState}.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class RabbitHeaderBinder implements RabbitAnnotatedArgumentBinder<Header> {

    private final ConversionService conversionService;

    /**
     * Default constructor.
     *
     * @param conversionService The conversion service to convert the body
     */
    public RabbitHeaderBinder(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Class<Header> getAnnotationType() {
        return Header.class;
    }

    @Override
    public BindingResult<Object> bind(ArgumentConversionContext<Object> context, RabbitConsumerState messageState) {
        String parameterName = context.getAnnotationMetadata()
                .getValue(Header.class, String.class)
                .orElse(context.getArgument().getName());

        Map<String, Object> rawHeaders = messageState.getProperties().getHeaders();

        RabbitHeaderConvertibleValues headers = new RabbitHeaderConvertibleValues(rawHeaders, conversionService);
        Optional<Object> header = headers.get(parameterName, context);
        return () -> header;
    }
}
