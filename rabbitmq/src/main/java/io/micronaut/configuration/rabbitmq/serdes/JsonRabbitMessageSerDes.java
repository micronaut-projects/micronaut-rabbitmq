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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.micronaut.configuration.rabbitmq.bind.RabbitConsumerState;
import io.micronaut.configuration.rabbitmq.intercept.MutableBasicProperties;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.serialize.exceptions.SerializationException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MediaType;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

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

    private final ObjectMapper objectMapper;

    /**
     * Default constructor.
     *
     * @param objectMapper The jackson object mapper
     */
    public JsonRabbitMessageSerDes(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object deserialize(RabbitConsumerState messageState, Argument<Object> type) {
        byte[] body = messageState.getBody();
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            if (type.hasTypeVariables()) {
                JavaType javaType = constructJavaType(type);
                return objectMapper.readValue(body, javaType);
            } else {
                return objectMapper.readValue(body, type.getType());
            }
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
            byte[] serialized = objectMapper.writeValueAsBytes(data);
            if (serialized != null && basicProperties.getContentType() == null) {
                basicProperties.setContentType(MediaType.APPLICATION_JSON);
            }
            return serialized;
        } catch (JsonProcessingException e) {
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

    private <T> JavaType constructJavaType(Argument<T> type) {
        Map<String, Argument<?>> typeVariables = type.getTypeVariables();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        JavaType[] objects = toJavaTypeArray(typeFactory, typeVariables);
        return typeFactory.constructParametricType(
                type.getType(),
                objects
        );
    }

    private JavaType[] toJavaTypeArray(TypeFactory typeFactory, Map<String, Argument<?>> typeVariables) {
        List<JavaType> javaTypes = new ArrayList<>();
        for (Argument<?> argument : typeVariables.values()) {
            if (argument.hasTypeVariables()) {
                javaTypes.add(typeFactory.constructParametricType(argument.getType(), toJavaTypeArray(typeFactory, argument.getTypeVariables())));
            } else {
                javaTypes.add(typeFactory.constructType(argument.getType()));
            }
        }
        return javaTypes.toArray(new JavaType[0]);
    }

}
