package io.micronaut.configuration.rabbitmq.docs.quickstart

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.util.concurrent.PollingConditions

class QuickstartSpec extends AbstractRabbitMQTest {

    void "test product client and listener"() {
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)

        when:
// tag::producer[]
def productClient = applicationContext.getBean(ProductClient)
productClient.send("message body".getBytes())
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        conditions.eventually {
            productListener.messageLengths.size() == 1
            productListener.messageLengths[0] == 12
        }

        cleanup:
        applicationContext.close()
    }
}
