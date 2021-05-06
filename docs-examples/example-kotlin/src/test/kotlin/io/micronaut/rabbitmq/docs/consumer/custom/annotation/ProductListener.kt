package io.micronaut.rabbitmq.docs.consumer.custom.annotation

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import java.util.Collections
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
