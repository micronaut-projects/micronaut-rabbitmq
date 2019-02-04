package io.micronaut.configuration.rabbitmq.docs.headers

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import io.micronaut.messaging.annotation.Headers

// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitClient
@Headers(
    Header(name = "x-product-sealed", value = "true"), // <1>
    Header(name = "x-product-size", value = "large")
)
interface ProductClient {

    @Binding("product")
    @Headers(
        Header(name = "x-product-count", value = "10"), // <2>
        Header(name = "x-product-size", value = "small")
    )
    fun send(data: ByteArray)

    @Binding("product")
    fun send(@Header("x-product-size") size: String?, // <3>
             @Header("x-product-count") count: Long,
             data: ByteArray)
}
// end::clazz[]