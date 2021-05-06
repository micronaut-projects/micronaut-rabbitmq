package io.micronaut.rabbitmq.docs.quickstart

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import java.util.Collections
// end::imports[]

@Requires(property = "spec.name", value = "QuickstartSpec")
// tag::clazz[]
@RabbitListener // <1>
class ProductListener {

    val messageLengths: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue("product") // <2>
    fun receive(data: ByteArray) { // <3>
        val string = String(data)
        messageLengths.add(string)
        println("Kotlin received ${data.size} bytes from RabbitMQ: ${string}")
    }
}
// end::clazz[]
