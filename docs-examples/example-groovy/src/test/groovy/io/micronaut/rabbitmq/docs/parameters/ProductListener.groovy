package io.micronaut.rabbitmq.docs.parameters

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "BindingSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    CopyOnWriteArrayList<Integer> messageLengths = []

    @Queue("product") // <1>
    void receive(byte[] data) {
        messageLengths << data.length
    }
}
// end::clazz[]
