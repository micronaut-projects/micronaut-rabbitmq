package io.micronaut.rabbitmq.docs.serdes

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class ProductInfoSerDesSpec extends AbstractRabbitMQTest {

    void "test using a custom serdes"() {
        startContext()

        when:
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send(new ProductInfo("small", 10L, true))
        productClient.send(new ProductInfo("medium", 20L, true))
        productClient.send(new ProductInfo(null, 30L, false))
// end::producer[]

        ProductListener listener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            listener.messages.size() == 3
            listener.messages.find({ p -> p.size == "small" && p.count == 10L && p.sealed }) != null
            listener.messages.find({ p -> p.size == "medium" && p.count == 20L && p.sealed }) != null
            listener.messages.find({ p -> p.size == null && p.count == 30L && !p.sealed }) != null
        }
    }
}
