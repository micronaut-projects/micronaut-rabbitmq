package io.micronaut.rabbitmq.docs.consumer.custom.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient

@Requires(property = "spec.name", value = "DeliveryTagSpec")
@RabbitClient
interface ProductClient {

    @Binding("product")
    fun send(data: ByteArray)
}
