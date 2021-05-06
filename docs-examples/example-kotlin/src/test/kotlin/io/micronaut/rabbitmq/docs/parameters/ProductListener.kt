package io.micronaut.rabbitmq.docs.parameters

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import java.util.Collections
// end::imports[]

@Requires(property = "spec.name", value = "BindingSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    val messageLengths: MutableList<Int> = Collections.synchronizedList(ArrayList())

    @Queue("product") // <1>
    fun receive(data: ByteArray) {
        messageLengths.add(data.size)
    }
}
// end::clazz[]
