package io.micronaut.configuration.rabbitmq.docs.headers

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitClient
@Header(name = "x-product-sealed", value = "true") // <1>
@Header(name = "x-product-size", value = "large")
interface ProductClient {

    @Binding("product")
    @Header(name = "x-product-count", value = "10") // <2>
    @Header(name = "x-product-size", value = "small")
    void send(byte[] data)

    @Binding("product")
    void send(@Header("x-product-size") String size, // <3>
              @Header("x-product-count") Long count,
              byte[] data)
}
// end::clazz[]