package io.micronaut.configuration.rabbitmq.serialization;

import io.micronaut.core.order.Ordered;

public interface RabbitMessageSerDes<T> extends Ordered {

    T deserialize(byte[] data, Class<T> type);

    byte[] serialize(T data);

    boolean supports(Class<T> type);
}
