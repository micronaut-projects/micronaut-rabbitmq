package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Connection
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class FailOverConnectionSpec extends AbstractRabbitMQTest {

    void "test multiple addresses"() {
        when:
        startContext('rabbitmq.addresses': ["${rabbitContainer.host}:62354", "${rabbitContainer.host}:${rabbitContainer.getMappedPort(5672)}"])

        then:
        applicationContext.getBean(Connection).port == rabbitContainer.getMappedPort(5672)
    }
}
