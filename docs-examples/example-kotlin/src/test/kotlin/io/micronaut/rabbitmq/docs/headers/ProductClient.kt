package io.micronaut.rabbitmq.docs.headers

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import io.micronaut.messaging.annotation.Headers
import io.micronaut.messaging.MessageHeaders
import io.micronaut.rabbitmq.annotation.RabbitHeaders

// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitClient
@Headers(
    Header(name = "x-product-sealed", value = "true"), // <1>
    Header(name = "productSize", value = "large")
)
interface ProductClient {

    @Binding("product")
    @Headers(
        Header(name = "x-product-count", value = "10"), // <2>
        Header(name = "productSize", value = "small")
    )
    fun send(data: ByteArray)

    @Binding("product")
    fun send(@Header productSize: String?, // <3>
             @Header("x-product-count") count: Long,
             data: ByteArray)

    @Binding("product")
    fun send(@RabbitHeaders headers: Map<String, Any>, // <4>
             data: ByteArray)
}
// end::clazz[]
