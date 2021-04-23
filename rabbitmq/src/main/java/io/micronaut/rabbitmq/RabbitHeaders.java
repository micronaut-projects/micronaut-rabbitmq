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
package io.micronaut.rabbitmq;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.messaging.MessageHeaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link MessageHeaders} implementation for Rabbit using a Map object.
 *
 * @author Raj Selvaraj
 * @since 2.4.1
 */
public class RabbitHeaders implements MessageHeaders {

    private final Map<String, String> headers;

    /**
     * Constructs a new instance for the given map of headers.
     *
     * @param headers A Map of headers
     */
    public RabbitHeaders(Map headers) {
        Objects.requireNonNull(headers, "Argument [headers] cannot be null");
        this.headers = headers;
    }

    @Override
    public List<String> getAll(CharSequence name) {
        if (name != null) {
            List<String> returnList = new ArrayList();
            returnList.add(this.headers.get(name.toString()));
            return returnList;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String get(CharSequence name) {
        if (this.headers != null) {
            return this.headers.get(name.toString());
        }
        return null;
    }

    @Override
    public Set<String> names() {
        return this.headers.keySet();
    }

    @Override
    public Collection<List<String>> values() {
        return names().stream().map(name -> {
            List<String> values = new ArrayList<>();
            values.add(new String(this.headers.get(name)));
            return values;
        }).collect(Collectors.toList());
    }

    @Override
    public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
        String v = get(name);
        if (v != null) {
            return ConversionService.SHARED.convert(v, conversionContext);
        }
        return Optional.empty();
    }

    @Override
    public RabbitHeaders add(CharSequence header, CharSequence value) {
        if (header != null && value != null) {
            this.headers.put(header.toString(), value.toString());
        }
        return this;
    }

    @Override
    public RabbitHeaders remove(CharSequence header) {
        if (header != null) {
            this.headers.remove(header.toString());
        }
        return this;
    }
}
