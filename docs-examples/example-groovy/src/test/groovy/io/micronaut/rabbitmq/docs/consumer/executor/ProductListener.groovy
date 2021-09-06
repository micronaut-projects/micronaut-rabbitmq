package io.micronaut.rabbitmq.docs.consumer.executor

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "CustomExecutorSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    CopyOnWriteArrayList<String> messageLengths = []

    @Queue(value = "product", executor = "product-listener") // <1>
    void receive(byte[] data) {
        messageLengths << new String(data)
    }
}
// end::clazz[]
