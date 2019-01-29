package io.micronaut.configuration.rabbitmq.serdes;

import java.util.Optional;

/**
 * A registry of {@link RabbitMessageSerDes} instances. Responsible
 * for returning the serdes that supports the given type.
 *
 * @see RabbitMessageSerDes#supports(Class)
 * @author James Kleeh
 * @since 1.1.0
 */
public interface RabbitMessageSerDesRegistry {

    /**
     * Returns the serdes that supports the given type.
     *
     * @param type The type
     * @param <T> The type to be serialized/deserialized
     * @return An optional serdes
     */
    <T> Optional<RabbitMessageSerDes<T>> findSerdes(Class<T> type);
}
