package io.micronaut.rabbitmq.docs.consumer.concurrent

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class ConcurrentSpec extends AbstractRabbitMQTest {

    void "test concurrent consumers"() {
        startContext()

        when:
        ProductClient productClient = applicationContext.getBean(ProductClient)
        4.times { productClient.send("body".bytes) }

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            productListener.threads.size() == 4
        }
    }
}
