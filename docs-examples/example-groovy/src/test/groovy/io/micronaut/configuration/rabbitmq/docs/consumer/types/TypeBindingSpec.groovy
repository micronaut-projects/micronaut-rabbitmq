package io.micronaut.configuration.rabbitmq.docs.consumer.types;

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.util.concurrent.PollingConditions

class TypeBindingSpec extends AbstractRabbitMQTest {

    void "test publishing and receiving headers"() {
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)

        when:
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send("body".bytes, "text/html")
        productClient.send("body2".bytes, "application/json")
        productClient.send("body3".bytes, "text/xml")
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        conditions.eventually {
            productListener.messages.size() == 3
            productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/html]")
            productListener.messages.contains("exchange: [], routingKey: [product], contentType: [application/json]")
            productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/xml]")
        }

        cleanup:
        applicationContext.close()
    }
}
