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
package io.micronaut.rabbitmq.serdes;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.serialize.exceptions.SerializationException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MediaType;
import io.micronaut.json.JsonMapper;
import io.micronaut.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.rabbitmq.intercept.MutableBasicProperties;
import jakarta.inject.Singleton;

import java.io.IOException;

/**
 * Serializes and deserializes objects as JSON using Jackson.
 *
 * @author James Kleeh
 * @since 1.1.0
 */
@Requires(bean = JsonMapper.class)
@Singleton
public class JsonRabbitMessageSerDes implements RabbitMessageSerDes<Object> {

    /**
     * The order of this serDes.
     */
    public static final Integer ORDER = 200;

    private final JsonMapper jsonMapper;

    /**
     * Default constructor.
     *
     * @param jsonMapper The json mapper
     * @since 3.2.0
     */
    public JsonRabbitMessageSerDes(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Object deserialize(RabbitConsumerState messageState, Argument<Object> type) {
        byte[] body = messageState.getBody();
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            return jsonMapper.readValue(body, type);
        } catch (IOException e) {
            throw new SerializationException("Error decoding JSON stream for type [" + type.getName() + "]: " + e.getMessage());
        }
    }

    @Override
    public byte[] serialize(Object data, MutableBasicProperties basicProperties) {
        if (data == null) {
            return null;
        }
        try {
            byte[] serialized = jsonMapper.writeValueAsBytes(data);
            if (serialized != null && basicProperties.getContentType() == null) {
                basicProperties.setContentType(MediaType.APPLICATION_JSON);
            }
            return serialized;
        } catch (IOException e) {
            throw new SerializationException("Error encoding object [" + data + "] to JSON: " + e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public boolean supports(Argument<Object> argument) {
        return !ClassUtils.isJavaBasicType(argument.getType());
    }
}
