package io.micronaut.configuration.rabbitmq.docs.serdes

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires

import java.util.ArrayList
import java.util.Collections
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSerDesSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    val messages: MutableList<ProductInfo> = Collections.synchronizedList(ArrayList())

    @Queue("product")
    fun receive(productInfo: ProductInfo) { // <1>
        messages.add(productInfo)
    }
}
// end::clazz[]