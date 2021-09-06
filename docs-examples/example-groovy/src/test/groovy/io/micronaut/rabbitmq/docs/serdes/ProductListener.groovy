package io.micronaut.rabbitmq.docs.serdes

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSerDesSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    CopyOnWriteArrayList<ProductInfo> messages = []

    @Queue("product")
    void receive(ProductInfo productInfo) { // <1>
        messages << productInfo
    }
}
// end::clazz[]
