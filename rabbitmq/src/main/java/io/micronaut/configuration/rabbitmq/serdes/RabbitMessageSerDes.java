package io.micronaut.configuration.rabbitmq.serdes;

import io.micronaut.core.order.Ordered;

/**
 * Responsible for serializing and deserializing RabbitMQ message bodies.
 *
 * @param <T> The type to be serialized/deserialized
 * @author James Kleeh
 * @since 1.1.0
 */
public interface RabbitMessageSerDes<T> extends Ordered {

    /**
     * Deserializes the message into the requested type.
     *
     * @param data The message body
     * @param type The type to be returned
     * @return The deserialized body
     */
    T deserialize(byte[] data, Class<T> type);

    /**
     * Serializes the data into a byte[] to be published
     * to RabbitMQ.
     *
     * @param data The data to serialize
     * @return The message body
     */
    byte[] serialize(T data);

    /**
     * Determines if this serdes supports the given type.
     *
     * @param type The type
     * @return True if the type is supported
     */
    boolean supports(Class<T> type);
}
