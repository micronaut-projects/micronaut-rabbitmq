package io.micronaut.configuration.rabbitmq.docs.parameters

import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

@Requires(property = "spec.name", value = "BindingSpec")
@RabbitListener
class ProductListener {

    List<Integer> messageLengths = Collections.synchronizedList([])

    @Queue("product")
    void receive(byte[] data) {
        messageLengths.add(data.length)
    }
}
