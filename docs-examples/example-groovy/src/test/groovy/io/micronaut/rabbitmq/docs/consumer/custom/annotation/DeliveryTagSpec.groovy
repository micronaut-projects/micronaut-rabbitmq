package io.micronaut.rabbitmq.docs.consumer.custom.annotation

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class DeliveryTagSpec extends AbstractRabbitMQTest {

    void "test using a custom annotation binder"() {
        startContext()

        when:
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send("body".bytes)
        productClient.send("body2".bytes)
        productClient.send("body3".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            assert productListener.messages.size() == 3
        }
    }
}
