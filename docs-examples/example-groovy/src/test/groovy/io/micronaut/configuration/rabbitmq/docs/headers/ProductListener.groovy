package io.micronaut.configuration.rabbitmq.docs.headers;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header

import javax.annotation.Nullable
// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<String> messageProperties = Collections.synchronizedList(new ArrayList<>())

    @Queue("product")
    void receive(byte[] data,
                 @Header("x-product-sealed") Boolean sealed, // <1>
                 @Header("x-product-count") Long count, // <2>
                 @Nullable @Header String productSize) { // <3>
        messageProperties.add(sealed.toString() + "|" + count + "|" + productSize)
    }
}
// end::clazz[]