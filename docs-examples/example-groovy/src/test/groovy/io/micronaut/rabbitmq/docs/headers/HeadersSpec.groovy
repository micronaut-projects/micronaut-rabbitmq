package io.micronaut.rabbitmq.docs.headers;

import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.util.concurrent.PollingConditions

class HeadersSpec extends AbstractRabbitMQTest {

    void "test publishing and receiving headers"() {

        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)

        when:
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send("body".bytes)
        productClient.send("medium", 20L, "body2".bytes)
        productClient.send(null, 30L, "body3".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        conditions.eventually {
            productListener.messageProperties.size() == 3
            productListener.messageProperties.contains("true|10|small")
            productListener.messageProperties.contains("true|20|medium")
            productListener.messageProperties.contains("true|30|null")
        }

        cleanup:
        applicationContext.close()
    }
}
