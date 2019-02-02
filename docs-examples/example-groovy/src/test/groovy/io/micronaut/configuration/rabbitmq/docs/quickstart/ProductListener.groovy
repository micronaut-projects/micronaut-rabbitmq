package io.micronaut.configuration.rabbitmq.docs.quickstart

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "QuickstartSpec")
// tag::class[]
@RabbitListener // <1>
class ProductListener {

    List<Integer> messageLengths = []

    @Queue("product") // <2>
    void receive(byte[] data) { // <3>
        Integer length = data.length
        messageLengths.add(length)
        println("Groovy received ${length} bytes from RabbitMQ")
    }
}
// end::class[]
