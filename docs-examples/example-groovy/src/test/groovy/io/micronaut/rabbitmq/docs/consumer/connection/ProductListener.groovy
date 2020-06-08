package io.micronaut.rabbitmq.docs.consumer.connection

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "ConnectionSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<String> messageLengths = Collections.synchronizedList([])

    @Queue(value = "product", connection = "product-cluster") // <1>
    void receive(byte[] data) {
        messageLengths.add(new String(data))
        System.out.println("Groovy received " + data.length + " bytes from RabbitMQ")
    }
}
// end::clazz[]
