package io.micronaut.configuration.rabbitmq.docs.parameters

import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

import java.util.ArrayList

@Requires(property = "spec.name", value = "BindingSpec")
@RabbitListener
class ProductListener {

    val messageLengths: MutableList<Int> = ArrayList()

    @Queue("product")
    fun receive(data: ByteArray) {
        messageLengths.add(data.size)
    }
}
