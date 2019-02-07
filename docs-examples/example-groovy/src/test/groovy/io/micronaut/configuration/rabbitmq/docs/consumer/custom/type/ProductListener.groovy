package io.micronaut.configuration.rabbitmq.docs.consumer.custom.type

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
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