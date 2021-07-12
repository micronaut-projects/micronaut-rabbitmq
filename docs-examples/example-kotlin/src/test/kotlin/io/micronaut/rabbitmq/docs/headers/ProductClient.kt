package io.micronaut.rabbitmq.docs.headers

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.messaging.annotation.MessageHeaders
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitHeaders
// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitClient
@MessageHeaders(
    MessageHeader(name = "x-product-sealed", value = "true"), // <1>
    MessageHeader(name = "productSize", value = "large")
)
interface ProductClient {

    @Binding("product")
    @MessageHeaders(
        MessageHeader(name = "x-product-count", value = "10"), // <2>
        MessageHeader(name = "productSize", value = "small")
    )
    fun send(data: ByteArray)

    @Binding("product")
    fun send(@MessageHeader productSize: String?, // <3>
             @MessageHeader("x-product-count") count: Long,
             data: ByteArray)

    @Binding("product")
    fun send(@RabbitHeaders headers: Map<String, Any>, // <4>
             data: ByteArray)
}
// end::clazz[]
