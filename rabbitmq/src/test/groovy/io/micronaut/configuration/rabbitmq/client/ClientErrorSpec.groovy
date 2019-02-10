package io.micronaut.configuration.rabbitmq.client

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.configuration.rabbitmq.annotation.RabbitProperty
import io.micronaut.configuration.rabbitmq.exception.RabbitClientException
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header

class ClientErrorSpec extends AbstractRabbitMQTest {

    void "test no body argument"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        Publisher publisher = applicationContext.getBean(Publisher)

        when:
        publisher.noBody("abc")

        then:
        def ex = thrown(RabbitClientException)
        ex.message.startsWith("No valid message body argument found")

        when:
        publisher.invalidProperty("abc")

        then:
        ex = thrown(RabbitClientException)
        ex.message == "Attempted to set property [xyz], but could not match the name to any of the com.rabbitmq.client.BasicProperties"

        when:
        publisher.invalidBody(TimeZone.getDefault())

        then:
        ex = thrown(RabbitClientException)
        ex.message == "Could not serialize the body argument of type [java.util.TimeZone] to a byte[] for publishing"

    }

    @Requires(property = "spec.name", value = "ClientErrorSpec")
    @RabbitClient
    static interface Publisher {

        void noBody(@Header String contentType)

        void invalidProperty(@RabbitProperty String xyz, byte[] body)

        void invalidBody(TimeZone body)
    }
}
