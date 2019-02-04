package io.micronaut.configuration.rabbitmq.docs.headers

import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import java.util.ArrayList
import java.util.Collections

@Requires(property = "spec.name", value = "HeadersSpec")
@RabbitListener
class ProductListener {

    var messageProperties: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue("product")
    fun receive(data: ByteArray,
                @Header("x-product-sealed") sealed: Boolean?,
                @Header("x-product-count") count: Long?,
                @Header("x-product-size") size: String?) {
        messageProperties.add(sealed.toString() + "|" + count + "|" + size)
    }
}
