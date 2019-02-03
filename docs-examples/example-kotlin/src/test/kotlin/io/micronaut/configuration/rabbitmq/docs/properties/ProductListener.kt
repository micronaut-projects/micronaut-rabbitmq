package io.micronaut.configuration.rabbitmq.docs.properties

import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.context.annotation.Requires
import java.util.*

@Requires(property = "spec.name", value = "PropertiesSpec")
@RabbitListener
class ProductListener {

    internal var messageProperties: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue("product")
    fun receive(data: ByteArray,
                @RabbitProperty("userId") user: String,
                @RabbitProperty contentType: String?,
                appId: String) {
        messageProperties.add("$user|$contentType|$appId")
    }
}
