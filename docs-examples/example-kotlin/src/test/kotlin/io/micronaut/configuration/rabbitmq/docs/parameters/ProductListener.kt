package io.micronaut.configuration.rabbitmq.docs.parameters

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
import java.util.*
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
