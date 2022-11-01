package io.micronaut.rabbitmq.docs.consumer.concurrent

// tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArraySet
// end::imports[]

@Requires(property = "spec.name", value = "ConcurrentSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    CopyOnWriteArraySet<String> threads = new CopyOnWriteArraySet<>()

    @Queue(value = "product", numberOfConcurrentConsumers = "5") // <1>
    void receive(byte[] data) {
        threads << Thread.currentThread().name // <2>
        sleep 500
    }
}
// end::clazz[]
