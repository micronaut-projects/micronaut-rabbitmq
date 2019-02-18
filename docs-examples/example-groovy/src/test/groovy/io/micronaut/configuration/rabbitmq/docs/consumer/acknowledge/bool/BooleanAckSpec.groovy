package io.micronaut.configuration.rabbitmq.docs.consumer.acknowledge.bool

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.lang.Ignore
import spock.util.concurrent.PollingConditions

@Ignore
class BooleanAckSpec extends AbstractRabbitMQTest {

    void "test acking with a boolean return"() {
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 10)

        when:
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send("body".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        conditions.eventually {
            productListener.messageCount.get() == 2
        }

        cleanup:
        applicationContext.close()
    }

}
