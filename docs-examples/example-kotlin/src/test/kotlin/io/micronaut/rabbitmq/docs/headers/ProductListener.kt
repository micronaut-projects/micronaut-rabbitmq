package io.micronaut.rabbitmq.docs.headers

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import java.util.ArrayList
import java.util.Collections
// end::imports[]

@Requires(property = "spec.name", value = "HeadersSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    var messageProperties: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Queue("product")
    fun receive(data: ByteArray,
                @Header("x-product-sealed") sealed: Boolean, // <1>
                @Header("x-product-count") count: Long, // <2>
                @Header productSize: String?) { // <3>
        messageProperties.add(sealed.toString() + "|" + count + "|" + productSize)
    }
}
// end::clazz[]