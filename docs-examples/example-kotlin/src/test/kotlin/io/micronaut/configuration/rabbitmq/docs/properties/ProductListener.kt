package io.micronaut.configuration.rabbitmq.docs.properties

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires
import java.util.*
// end::imports[]

@Requires(property = "spec.name", value = "PropertiesSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    val messageProperties: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue("product")
    @RabbitProperty(name = "x-priority", value = "10", type = Integer::class) // <1>
    fun receive(data: ByteArray,
                @RabbitProperty("userId") user: String, // <2>
                @RabbitProperty contentType: String?, // <3>
                appId: String) { // <4>
        messageProperties.add("$user|$contentType|$appId")
    }
}
// end::clazz[]