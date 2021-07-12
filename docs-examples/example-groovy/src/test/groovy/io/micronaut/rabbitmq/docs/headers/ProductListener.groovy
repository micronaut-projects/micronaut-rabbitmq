package io.micronaut.rabbitmq.docs.headers

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.core.annotation.Nullable
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitHeaders
import io.micronaut.rabbitmq.annotation.RabbitListener
// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<String> messageProperties = Collections.synchronizedList(new ArrayList<>())

    @Queue("product")
    void receive(byte[] data,
                 @MessageHeader("x-product-sealed") Boolean sealed, // <1>
                 @MessageHeader("x-product-count") Long count, // <2>
                 @Nullable @MessageHeader String productSize) { // <3>
        messageProperties.add(sealed.toString() + "|" + count + "|" + productSize)
    }

    @Queue("product")
    void receive(byte[] data,
                 @RabbitHeaders Map<String, Object> headers) { // <4>
        messageProperties.add(
                headers["x-product-sealed"].toString() + "|" +
                headers["x-product-count"].toString() + "|" +
                headers["productSize"]?.toString())
    }
}
// end::clazz[]
