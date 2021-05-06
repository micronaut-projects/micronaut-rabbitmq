package io.micronaut.rabbitmq.docs.serdes

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.messaging.annotation.Body
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSerDesSpec")
// tag::clazz[]
@RabbitClient
interface ProductClient {

    @Binding("product")
    fun send(@Body data: ProductInfo)
}
// end::clazz[]
