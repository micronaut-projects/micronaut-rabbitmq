package io.micronaut.configuration.rabbitmq.serialization;

import java.util.Optional;

public interface RabbitMessageSerDesRegistry {

    <T> Optional<RabbitMessageSerDes<T>> findSerdes(Class<T> type);
}
