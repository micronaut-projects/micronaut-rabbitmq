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

package io.micronaut.configuration.rabbitmq.serdes;

import io.micronaut.configuration.rabbitmq.bind.RabbitMessageState;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.serialize.exceptions.SerializationException;
import io.micronaut.jackson.serialize.JacksonObjectSerializer;

import javax.inject.Singleton;
import java.util.Arrays;

/**
 * Serializes and deserializes objects as JSON using Jackson.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Singleton
public class JsonRabbitMessageSerDes implements RabbitMessageSerDes<Object> {

    /**
     * The order of this serDes.
     */
    public static final Integer ORDER = 200;

    private final JacksonObjectSerializer objectSerializer;

    /**
     * Default constructor.
     *
     * @param objectSerializer The jackson serializer/deserializer
     */
    public JsonRabbitMessageSerDes(JacksonObjectSerializer objectSerializer) {
        this.objectSerializer = objectSerializer;
    }

    @Override
    public Object deserialize(RabbitMessageState messageState, Class<Object> type) {
        byte[] body = messageState.getBody();
        return objectSerializer.deserialize(body, type)
                .orElseThrow(() -> new SerializationException("Unable to deserialize data: " + Arrays.toString(body)));
    }

    @Override
    public byte[] serialize(Object data) {
        return objectSerializer.serialize(data)
                .orElseThrow(() -> new SerializationException("Unable to serialize data: " + data.getClass()));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public boolean supports(Class<Object> type) {
        return !ClassUtils.isJavaBasicType(type);
    }

}
