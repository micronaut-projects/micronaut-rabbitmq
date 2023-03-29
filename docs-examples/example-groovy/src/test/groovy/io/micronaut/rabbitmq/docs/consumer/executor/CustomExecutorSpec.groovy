package io.micronaut.rabbitmq.docs.consumer.executor

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class CustomExecutorSpec extends AbstractRabbitMQTest {

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
            assert productListener.messageLengths.size() == 1
            assert productListener.messageLengths[0] == "custom-executor-test"
        }
    }

    protected Map<String, Object> getConfiguration() {
        super.configuration + ["micronaut.executors.product-listener.type": "FIXED"]
    }
}
