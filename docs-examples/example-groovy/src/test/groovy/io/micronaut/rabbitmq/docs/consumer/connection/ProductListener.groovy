package io.micronaut.rabbitmq.docs.consumer.connection

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
// end::imports[]

@Requires(property = "spec.name", value = "ConnectionSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<String> messageLengths = Collections.synchronizedList([])

    @Queue(value = "product", connection = "product-cluster") // <1>
    void receive(byte[] data) {
        messageLengths << new String(data)
    }
}
// end::clazz[]
