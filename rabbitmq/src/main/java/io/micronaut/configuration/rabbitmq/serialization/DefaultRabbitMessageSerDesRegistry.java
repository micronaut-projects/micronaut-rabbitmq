package io.micronaut.configuration.rabbitmq.serialization;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Optional;

@Singleton
public class DefaultRabbitMessageSerDesRegistry implements RabbitMessageSerDesRegistry {

    private final RabbitMessageSerDes<?>[] serDes;

    public DefaultRabbitMessageSerDesRegistry(RabbitMessageSerDes<?>... serDes) {
        this.serDes = serDes;
    }

    @Override
    public <T> Optional<RabbitMessageSerDes<T>> findSerdes(Class<T> type) {
        return Arrays.stream(serDes)
                .filter(serDes -> serDes.supports((Class) type))
                .map(serDes -> (RabbitMessageSerDes<T>) serDes)
                .findFirst();
    }
}
