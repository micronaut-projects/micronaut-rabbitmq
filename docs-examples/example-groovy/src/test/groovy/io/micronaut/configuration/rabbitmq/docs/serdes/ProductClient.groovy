package io.micronaut.configuration.rabbitmq.docs.serdes

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Binding
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Body
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSerDesSpec")
// tag::clazz[]
@RabbitClient
interface ProductClient {

    @Binding("product")
    void send(@Body ProductInfo data)
}
// end::clazz[]