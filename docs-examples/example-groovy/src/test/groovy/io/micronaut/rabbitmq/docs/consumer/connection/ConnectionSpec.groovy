package io.micronaut.rabbitmq.docs.consumer.connection

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class ConnectionSpec extends AbstractRabbitMQTest {

    void "test product client and listener"() {
        startContext()

        when:
// tag::producer[]
        def productClient = applicationContext.getBean(ProductClient)
        productClient.send("connection-test".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            productListener.messageLengths.size() == 1
            productListener.messageLengths[0] == "connection-test"
        }

        cleanup:
        // Finding that the context is closing the channel before ack is sent
        sleep 200
    }

    protected Map<String, Object> getConfiguration() {
        super.configuration + ["rabbitmq.servers.product-cluster.port": super.configuration.remove("rabbitmq.port")]
    }
}
