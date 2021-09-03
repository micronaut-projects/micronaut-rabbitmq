package io.micronaut.rabbitmq.docs.quickstart

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class QuickstartSpec extends AbstractRabbitMQTest {

    void "test product client and listener"() {
        startContext()

        when:
// tag::producer[]
def productClient = applicationContext.getBean(ProductClient)
productClient.send("quickstart".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            productListener.messageLengths.size() == 1
            productListener.messageLengths[0] == "quickstart"
        }

        cleanup:
        // Finding that the context is closing the channel before ack is sent
        sleep 200
    }
}
