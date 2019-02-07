package io.micronaut.configuration.rabbitmq.docs.consumer.acknowledge.type

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.util.concurrent.PollingConditions

class AcknowledgeSpec extends AbstractRabbitMQTest {

    void "test acking with an acknowledgement argument"() {
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 10)

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
        conditions.eventually {
            productListener.messageCount.get() == 5
        }

        cleanup:
        applicationContext.close()
    }
}
