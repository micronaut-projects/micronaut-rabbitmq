package io.micronaut.rabbitmq.docs.consumer.types

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class TypeBindingSpec extends AbstractRabbitMQTest {

    void "test publishing and receiving rabbitmq types"() {
        startContext()

        when:
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send("body".bytes, "text/html")
        productClient.send("body2".bytes, "application/json")
        productClient.send("body3".bytes, "text/xml")
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            productListener.messages.size() == 3
            productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/html]")
            productListener.messages.contains("exchange: [], routingKey: [product], contentType: [application/json]")
            productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/xml]")
        }
    }
}
