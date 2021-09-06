package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Connection
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class FailOverConnectionSpec extends AbstractRabbitMQTest {

    void "test multiple addresses"() {
        when:
        startContext('rabbitmq.addresses': ['localhost:62354', "localhost:${rabbitContainer.getMappedPort(5672)}"])

        then:
        applicationContext.getBean(Connection).port == rabbitContainer.getMappedPort(5672)
    }
}
