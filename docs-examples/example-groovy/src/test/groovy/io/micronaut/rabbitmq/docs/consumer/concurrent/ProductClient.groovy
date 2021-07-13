package io.micronaut.rabbitmq.docs.consumer.concurrent

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.RabbitClient

@Requires(property = "spec.name", value = "ConcurrentSpec")
@RabbitClient
interface ProductClient {

    @Binding("product")
    void send(byte[] data)
}
