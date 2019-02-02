package io.micronaut.configuration.rabbitmq.docs.quickstart

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

import java.util.ArrayList
// end::imports[]

@Requires(property = "spec.name", value = "QuickstartSpec")
// tag::class[]
@RabbitListener // <1>
class ProductListener {

    val messageLengths: MutableList<Int> = ArrayList()

    @Queue("product") // <2>
    fun receive(data: ByteArray) { // <3>
        val length = data.size
        messageLengths.add(length)
        println("Kotlin received $length bytes from RabbitMQ")
    }
}
// end::class[]