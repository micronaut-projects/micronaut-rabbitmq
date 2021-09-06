package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    CopyOnWriteArrayList<ProductInfo> messages = []

    @Queue("product")
    void receive(byte[] data,
                 ProductInfo productInfo) { // <1>
        messages << productInfo
    }
}
// end::clazz[]
