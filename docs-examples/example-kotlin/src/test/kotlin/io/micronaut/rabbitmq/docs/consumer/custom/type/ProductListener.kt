package io.micronaut.rabbitmq.docs.consumer.custom.type

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

import java.util.ArrayList
import java.util.Collections
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    val messages: MutableList<ProductInfo> = Collections.synchronizedList(ArrayList())

    @Queue("product")
    fun receive(data: ByteArray,
                productInfo: ProductInfo) { // <1>
        messages.add(productInfo)
    }
}
// end::clazz[]