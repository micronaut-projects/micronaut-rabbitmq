package io.micronaut.configuration.rabbitmq.docs.headers;

import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header

import javax.annotation.Nullable

@Requires(property = "spec.name", value = "HeadersSpec")
@RabbitListener
class ProductListener {

    List<String> messageProperties = Collections.synchronizedList(new ArrayList<>())

    @Queue("product")
    void receive(byte[] data,
                 @Header("x-product-sealed") Boolean sealed,
                 @Header("x-product-count") Long count,
                 @Nullable @Header("x-product-size") String size) {
        messageProperties.add(sealed.toString() + "|" + count + "|" + size)
    }
}
