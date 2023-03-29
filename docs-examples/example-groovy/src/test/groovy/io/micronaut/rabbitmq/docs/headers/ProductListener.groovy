package io.micronaut.rabbitmq.docs.headers

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.core.annotation.Nullable
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitHeaders
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    CopyOnWriteArrayList<String> messageProperties = []

    @Queue("product")
    void receive(byte[] data,
                 @MessageHeader("x-product-sealed") Boolean productSealed, // <1>
                 @MessageHeader("x-product-count") Long count, // <2>
                 @Nullable @MessageHeader String productSize) { // <3>
        messageProperties << productSealed.toString() + "|" + count + "|" + productSize
    }

    @Queue("product")
    void receive(byte[] data,
                 @RabbitHeaders Map<String, Object> headers) { // <4>
        messageProperties <<
                headers["x-product-sealed"].toString() + "|" +
                headers["x-product-count"] + "|" +
                headers["productSize"]?.toString()
    }
}
// end::clazz[]
