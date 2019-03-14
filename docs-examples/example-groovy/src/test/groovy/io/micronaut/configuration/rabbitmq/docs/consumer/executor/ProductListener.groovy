package io.micronaut.configuration.rabbitmq.docs.consumer.executor

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "CustomExecutorSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<String> messageLengths = Collections.synchronizedList([])

    @Queue(value = "product", executor = "product-listener") // <1>
    void receive(byte[] data) {
        messageLengths.add(new String(data))
        System.out.println("Groovy received " + data.length + " bytes from RabbitMQ")
    }
}
// end::clazz[]
