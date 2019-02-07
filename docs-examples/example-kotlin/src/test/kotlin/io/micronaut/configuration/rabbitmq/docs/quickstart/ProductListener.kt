package io.micronaut.configuration.rabbitmq.docs.quickstart

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
import java.util.*

// end::imports[]

@Requires(property = "spec.name", value = "QuickstartSpec")
// tag::clazz[]
@RabbitListener // <1>
class ProductListener {

    val messageLengths: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue("product") // <2>
    fun receive(data: ByteArray) { // <3>
        messageLengths.add(String(data))
        println("Kotlin received ${data.size} bytes from RabbitMQ")
    }
}
// end::clazz[]