package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Connection
import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class FailOverConnectionSpec extends AbstractRabbitMQTest {

    void "test multiple addresses"() {
        ApplicationContext ctx = startContext(['rabbitmq.addresses': ['localhost:62354', "localhost:${rabbitContainer.getMappedPort(5672)}"]])

        expect:
        ctx.getBean(Connection).getPort() == rabbitContainer.getMappedPort(5672)
    }
}
