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
    var threads = CopyOnWriteArraySet<String>()

    @Queue(value = "product", numberOfConsumers = "5") // <1>
    fun receive(data: ByteArray?) {
        threads.add(Thread.currentThread().name) // <2>
        Thread.sleep(500)
    }
}
// end::clazz[]
