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
package io.micronaut.rabbitmq.bind;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.ConvertibleValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Converts RabbitMQ header values to the requested type.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
public class RabbitHeaderConvertibleValues implements ConvertibleValues<Object> {

    private final Map<String, Object> headers;
    private final ConversionService conversionService;
    private final List<ConversionError> conversionErrors = new ArrayList<>();

    /**
     * Default constructor.
     *
     * @param headers The RabbitMQ headers
     * @param conversionService The conversion service
     */
    public RabbitHeaderConvertibleValues(Map<String, Object> headers, ConversionService conversionService) {
        this.headers = headers == null ? Collections.emptyMap() : headers;
        this.conversionService = conversionService;
    }

    @Override
    public Set<String> names() {
        return headers.keySet();
    }

    @Override
    public Collection<Object> values() {
        return headers.values();
    }

    @Override
    public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
        Object value = headers.get(name.toString());
        if (value != null) {
            Optional<T> converted = conversionService.convert(value.toString(), conversionContext);
            conversionContext.getLastError().ifPresent(conversionErrors::add);
            return converted;
        }
        return Optional.empty();
    }

    /**
     * @return Any conversion errors that may have occurred
     */
    public List<ConversionError> getConversionErrors() {
        return conversionErrors;
    }
}
