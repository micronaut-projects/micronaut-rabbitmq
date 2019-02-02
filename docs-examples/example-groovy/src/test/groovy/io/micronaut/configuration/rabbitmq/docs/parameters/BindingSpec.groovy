package io.micronaut.configuration.rabbitmq.docs.parameters

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.util.concurrent.PollingConditions

class BindingSpec extends AbstractRabbitMQTest {

    void "test dynamic binding"() {
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)

        when:
// tag::producer[]
        def productClient = applicationContext.getBean(ProductClient)
        productClient.send("message body".bytes)
        productClient.send("product", "message body2".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        conditions.eventually {
            productListener.messageLengths.size() == 2
            productListener.messageLengths.contains(12)
            productListener.messageLengths.contains(13)
        }

        cleanup:
        applicationContext.close()
    }
}
