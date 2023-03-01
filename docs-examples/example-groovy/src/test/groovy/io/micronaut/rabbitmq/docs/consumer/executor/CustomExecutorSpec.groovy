package io.micronaut.rabbitmq.docs.consumer.executor

import io.micronaut.rabbitmq.AbstractRabbitMQTest
import spock.lang.Ignore

class CustomExecutorSpec extends AbstractRabbitMQTest {

    @Ignore("this just waits indefinitely and never runs to completion")
    void "test product client and listener"() {
        startContext()

        when:
// tag::producer[]
        def productClient = applicationContext.getBean(ProductClient)
        productClient.send("custom-executor-test".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            productListener.messageLengths.size() == 1
            productListener.messageLengths[0] == "custom-executor-test"
        }

        cleanup:
        // Finding that the context is closing the channel before ack is sent
        sleep 200
    }

    protected Map<String, Object> getConfiguration() {
        super.configuration + ["micronaut.executors.product-listener.type": "FIXED"]
    }
}
