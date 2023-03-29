package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class ProductInfoSpec extends AbstractRabbitMQTest {

    void "test using a custom type binder"() {
        startContext()

        when:
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send("body".bytes)
        productClient.send("medium", 20L, "body2".bytes)
        productClient.send(null, 30L, "body3".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            assert productListener.messages.size() == 3

            assert productListener.messages.find({ pi ->
                pi.size == "small" && pi.count == 10 && pi.sealed
            }) != null

            assert productListener.messages.find({ pi ->
                pi.size == "medium" && pi.count == 20 && pi.sealed
            }) != null

            assert productListener.messages.find({ pi ->
                pi.size == null && pi.count == 30 && pi.sealed
            }) != null
        }
    }
}
