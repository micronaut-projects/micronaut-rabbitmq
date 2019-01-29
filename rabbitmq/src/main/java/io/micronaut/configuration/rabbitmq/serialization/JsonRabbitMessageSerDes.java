package io.micronaut.configuration.rabbitmq.serialization;

import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.serialize.exceptions.SerializationException;
import io.micronaut.jackson.serialize.JacksonObjectSerializer;

import javax.inject.Singleton;
import java.util.Arrays;

@Singleton
public class JsonRabbitMessageSerDes<T> implements RabbitMessageSerDes<T> {

    private final JacksonObjectSerializer objectSerializer;

    public JsonRabbitMessageSerDes(JacksonObjectSerializer objectSerializer) {
        this.objectSerializer = objectSerializer;
    }

    @Override
    public T deserialize(byte[] body, Class<T> type) {
        return objectSerializer.deserialize(body, type)
                .orElseThrow(() -> new SerializationException("Unable to deserialize data: " + Arrays.toString(body)));
    }

    @Override
    public byte[] serialize(T data) {
        return objectSerializer.serialize(data)
                .orElseThrow(() -> new SerializationException("Unable to serialize data: " + data.getClass()));
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public boolean supports(Class<T> type) {
        return !ClassUtils.isJavaBasicType(type);
    }
}
