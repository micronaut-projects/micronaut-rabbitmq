package io.micronaut.rabbitmq.docs.serdes

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
// end::imports[]

@Requires(property = "spec.name", value = "ProductInfoSerDesSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    List<ProductInfo> messages = Collections.synchronizedList(new ArrayList<>());

    @Queue("product")
    public void receive(ProductInfo productInfo) { // <1>
        messages.add(productInfo);
    }
}
// end::clazz[]
