package io.micronaut.rabbitmq.docs.consumer.concurrent

import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import spock.util.concurrent.PollingConditions

class ConcurrentSpec extends AbstractRabbitMQTest {

    void "test concurrent consumers"() {
        ApplicationContext applicationContext = startContext()
        ProductClient productClient = applicationContext.getBean(ProductClient.class)
        5.times { productClient.send("body".getBytes()) }

        ProductListener productListener = applicationContext.getBean(ProductListener.class)
        PollingConditions conditions = new PollingConditions(timeout: 5)

        conditions.eventually {
            productListener.threads.size() == 5
        }

        cleanup:
        applicationContext.close()
    }
}
