package io.micronaut.rabbitmq.docs.publisher.acknowledge

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "PublisherAcknowledgeSpec")
// tag::clazz[]
@RabbitListener // <1>
class ProductListener {

    CopyOnWriteArrayList<Integer> messageLengths = []

    @Queue("product") // <2>
    void receive(byte[] data) { // <3>
        messageLengths << data.length
    }
}
// end::clazz[]
