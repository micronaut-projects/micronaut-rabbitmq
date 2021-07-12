package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSpec")
// tag::clazz[]
@RabbitClient
@MessageHeader(name = "x-product-sealed", value = "true") // <1>
@MessageHeader(name = "productSize", value = "large")
interface ProductClient {

    @Binding("product")
    @MessageHeader(name = "x-product-count", value = "10") // <2>
    @MessageHeader(name = "productSize", value = "small")
    void send(byte[] data)

    @Binding("product")
    void send(@MessageHeader String productSize, // <3>
              @MessageHeader("x-product-count") Long count,
              byte[] data)
}
// end::clazz[]
