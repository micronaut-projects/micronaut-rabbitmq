package io.micronaut.rabbitmq.docs.parameters

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class BindingSpec extends AbstractRabbitMQTest {

    void "test dynamic binding"() {
        startContext()

        when:
// tag::producer[]
        def productClient = applicationContext.getBean(ProductClient)
        productClient.send("message body".bytes)
        productClient.send("product", "message body2".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            assert productListener.messageLengths.size() == 2
            assert productListener.messageLengths.contains(12)
            assert productListener.messageLengths.contains(13)
        }
    }
}
