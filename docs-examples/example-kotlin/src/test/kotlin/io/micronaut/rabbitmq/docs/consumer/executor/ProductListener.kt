package io.micronaut.rabbitmq.docs.consumer.executor

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import java.util.Collections
// end::imports[]

@Requires(property = "spec.name", value = "CustomExecutorSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    internal var messageLengths: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue(value = "product", executor = "product-listener") // <1>
    fun receive(data: ByteArray) {
        messageLengths.add(String(data))
        println("Kotlin received " + data.size + " bytes from RabbitMQ")
    }
}
// end::clazz[]
