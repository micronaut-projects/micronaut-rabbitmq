package io.micronaut.configuration.rabbitmq.docs.quickstart

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "QuickstartSpec")
// tag::clazz[]
@RabbitListener // <1>
class ProductListener {

    List<String> messageLengths = Collections.synchronizedList([])

    @Queue("product") // <2>
    void receive(byte[] data) { // <3>
        messageLengths.add(new String(data))
        println("Groovy received ${data.length} bytes from RabbitMQ")
    }
}
// end::clazz[]
