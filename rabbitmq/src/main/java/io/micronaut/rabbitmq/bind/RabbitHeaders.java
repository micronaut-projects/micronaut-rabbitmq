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

/**
 * A {@link MessageHeaders} implementation for Rabbit using a Map object.
 *
 * @author Raj Selvaraj
 * @since 2.4.1
 */
public class RabbitHeaders implements MessageHeaders {

    private final Map<String, Object> headers;

    /**
     * Constructs a new instance for the given map of headers.
     *
     * @param headers A Map of headers
     */
    public RabbitHeaders(Map<String, Object> headers) {
        Objects.requireNonNull(headers, "Argument [headers] cannot be null");
        this.headers = headers;
    }

    @Override
    public List getAll(CharSequence name) {
        if (name != null) {
            List returnList = new ArrayList();
            Object value = this.headers.get(name.toString());
            if (value != null) {
                returnList.add(value);
            }
            return returnList;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String get(CharSequence name) {
        if (this.headers != null) {
            Object value = this.headers.get(name.toString());
            if (value != null && value instanceof String) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * Get  the  Object from the headers for the given name.
     * @param name
     * @return Object
     */
    public Object get(String name) {
        if (this.headers != null) {
            return this.headers.get(name);
        }
        return null;
    }

    @Override
    public Set<String> names() {
        return this.headers.keySet();
    }

    @Override
    public Collection values() {
        return this.headers.values();
    }

    @Override
    public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
        Object v = get(name);
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

    /**
     * Add a header for the given name and Object value.
     *
     * @param header The head name
     * @param value  The value object
     * @return This headers object
     */
    public RabbitHeaders add(CharSequence header, Object value) {
        if (header != null && value != null) {
            this.headers.put(header.toString(), value);
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
