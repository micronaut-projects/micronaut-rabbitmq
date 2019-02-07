package io.micronaut.configuration.rabbitmq.docs.consumer.custom.type

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.util.concurrent.PollingConditions

class ProductInfoSpec extends AbstractRabbitMQTest {

    void "test using a custom type binder"() {
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
            productListener.messages.size() == 3
            productListener.messages.find({ pi ->
                pi.size == "small" && pi.count == 10 && pi.sealed
            }) != null
            productListener.messages.find({ pi ->
                pi.size == "medium" && pi.count == 20 && pi.sealed
            }) != null
            productListener.messages.find({ pi ->
                pi.size == null && pi.count == 30 && pi.sealed
            }) != null
        }

        cleanup:
        applicationContext.close()
    }
}