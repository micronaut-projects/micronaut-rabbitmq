package io.micronaut.rabbitmq.docs.quickstart

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "QuickstartSpec")
// tag::clazz[]
@RabbitListener // <1>
class ProductListener {

    CopyOnWriteArrayList<String> messageLengths = []

    @Queue("product") // <2>
    void receive(byte[] data) { // <3>
        messageLengths << new String(data)
    }
}
// end::clazz[]
