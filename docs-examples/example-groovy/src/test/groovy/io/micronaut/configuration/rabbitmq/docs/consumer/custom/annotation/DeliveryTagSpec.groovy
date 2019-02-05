package io.micronaut.configuration.rabbitmq.docs.consumer.custom.annotation

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.util.concurrent.PollingConditions

class DeliveryTagSpec extends AbstractRabbitMQTest {

    void "test using a custom annotation binder"() {
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)

        when:
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send("body".bytes)
        productClient.send("body2".bytes)
        productClient.send("body3".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        conditions.eventually {
            productListener.messages.size() == 3
        }

        cleanup:
        applicationContext.close()
    }
}
