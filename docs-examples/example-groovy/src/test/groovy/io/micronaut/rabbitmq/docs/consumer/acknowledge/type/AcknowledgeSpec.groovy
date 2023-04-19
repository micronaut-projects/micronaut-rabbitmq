package io.micronaut.rabbitmq.docs.consumer.acknowledge.type

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class AcknowledgeSpec extends AbstractRabbitMQTest {

    void "test acking with an acknowledgement argument"() {
        startContext()

        when:
// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient)
productClient.send("message body".bytes)
productClient.send("message body".bytes)
productClient.send("message body".bytes)
productClient.send("message body".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            assert productListener.messageCount.get() == 5
        }
    }
}
