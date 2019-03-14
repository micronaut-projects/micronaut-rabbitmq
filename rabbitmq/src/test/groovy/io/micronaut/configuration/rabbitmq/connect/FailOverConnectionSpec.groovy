package io.micronaut.configuration.rabbitmq.connect

import com.rabbitmq.client.Connection
import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext

class FailOverConnectionSpec extends AbstractRabbitMQTest {

    void "test multiple addresses"() {
        ApplicationContext ctx = startContext(['rabbitmq.addresses': ['localhost:62354', "localhost:${rabbitContainer.getMappedPort(5672)}"]])

        expect:
        ctx.getBean(Connection).getPort() == rabbitContainer.getMappedPort(5672)
    }
}
