package io.micronaut.configuration.rabbitmq.docs.consumer.custom.type

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import io.micronaut.messaging.annotation.Headers
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSpec")
// tag::clazz[]
@RabbitClient
@Headers( // <1>
        Header(name = "x-product-sealed", value = "true"),
        Header(name = "productSize", value = "large")
)
interface ProductClient {

    @Binding("product")
    @Headers( // <2>
        Header(name = "x-product-count", value = "10"),
        Header(name = "productSize", value = "small")
    )
    fun send(data: ByteArray)

    @Binding("product")
    fun send(@Header productSize: String?, // <3>
             @Header("x-product-count") count: Long,
             data: ByteArray)
}
// end::clazz[]