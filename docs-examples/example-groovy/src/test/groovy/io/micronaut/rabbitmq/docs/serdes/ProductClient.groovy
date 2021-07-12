package io.micronaut.rabbitmq.docs.serdes

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSerDesSpec")
// tag::clazz[]
@RabbitClient
interface ProductClient {

    @Binding("product")
    void send(@MessageBody ProductInfo data)
}
// end::clazz[]
