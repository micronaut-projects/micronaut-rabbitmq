package io.micronaut.configuration.rabbitmq.docs.consumer.connection

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

import java.util.ArrayList
import java.util.Collections

// end::imports[]

@Requires(property = "spec.name", value = "ConnectionSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    internal var messageLengths: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue(value = "product", connection = "product-cluster") // <1>
    fun receive(data: ByteArray) {
        messageLengths.add(String(data))
        println("Java received " + data.size + " bytes from RabbitMQ")
    }
}
// end::clazz[]
