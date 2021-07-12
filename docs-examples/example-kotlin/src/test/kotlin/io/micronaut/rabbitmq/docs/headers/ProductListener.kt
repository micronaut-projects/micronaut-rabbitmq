package io.micronaut.rabbitmq.docs.headers

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitHeaders
import io.micronaut.rabbitmq.annotation.RabbitListener
import java.util.Collections
// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    var messageProperties: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue("product")
    fun receive(data: ByteArray,
                @MessageHeader("x-product-sealed") sealed: Boolean, // <1>
                @MessageHeader("x-product-count") count: Long, // <2>
                @MessageHeader productSize: String?) { // <3>
        messageProperties.add(sealed.toString() + "|" + count + "|" + productSize)
    }

    @Queue("product")
    fun receive(data: ByteArray,
                @RabbitHeaders headers: Map<String, Any>) { // <4>
        messageProperties.add(
            headers["x-product-sealed"].toString() + "|" +
            headers["x-product-count"].toString() + "|" +
            headers["productSize"]?.toString()
        )
    }
}
// end::clazz[]
