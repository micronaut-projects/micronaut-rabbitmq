package io.micronaut.rabbitmq.docs.consumer.custom.annotation

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArraySet
// end::imports[]

@Requires(property = "spec.name", value = "DeliveryTagSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    CopyOnWriteArraySet<Long> messages = []

    @Queue("product")
    void receive(byte[] data, @DeliveryTag Long tag) { // <1>
        messages << tag
    }
}
// end::clazz[]
