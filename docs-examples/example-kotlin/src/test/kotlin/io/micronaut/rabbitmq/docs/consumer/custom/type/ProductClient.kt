package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.messaging.annotation.MessageHeaders
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSpec")
// tag::clazz[]
@RabbitClient
@MessageHeaders( // <1>
        MessageHeader(name = "x-product-sealed", value = "true"),
        MessageHeader(name = "productSize", value = "large")
)
interface ProductClient {

    @Binding("product")
    @MessageHeaders( // <2>
        MessageHeader(name = "x-product-count", value = "10"),
        MessageHeader(name = "productSize", value = "small")
    )
    fun send(data: ByteArray)

    @Binding("product")
    fun send(@MessageHeader productSize: String?, // <3>
             @MessageHeader("x-product-count") count: Long,
             data: ByteArray)
}
// end::clazz[]
