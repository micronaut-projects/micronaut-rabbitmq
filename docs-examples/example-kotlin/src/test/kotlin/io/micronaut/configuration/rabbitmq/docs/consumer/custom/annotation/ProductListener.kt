package io.micronaut.configuration.rabbitmq.docs.consumer.custom.annotation

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

import java.util.Collections
import java.util.HashSet
// end::imports[]

@Requires(property = "spec.name", value = "DeliveryTagSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    val messages: MutableSet<Long> = Collections.synchronizedSet(HashSet())

    @Queue("product")
    fun receive(data: ByteArray, @DeliveryTag tag: Long) { // <1>
        messages.add(tag)
    }
}
// end::clazz[]
