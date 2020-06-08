package io.micronaut.rabbitmq.docs.consumer.custom.type

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSpec")
// tag::clazz[]
@RabbitListener
class ProductListener {

    List<ProductInfo> messages = Collections.synchronizedList([])

    @Queue("product")
    void receive(byte[] data,
                 ProductInfo productInfo) { // <1>
        messages.add(productInfo)
    }
}
// end::clazz[]