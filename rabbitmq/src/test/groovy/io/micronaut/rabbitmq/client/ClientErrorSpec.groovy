package io.micronaut.rabbitmq.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Body
import io.micronaut.messaging.annotation.Header
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty
import io.micronaut.rabbitmq.exception.RabbitClientException
import io.reactivex.Completable
import spock.util.concurrent.PollingConditions

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
        ex.message == "Could not find a serializer for the body argument of type [java.util.TimeZone]"
    }

    void "test publishing to an exchange that does not exist"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PublisherInvalidExchange publisher = applicationContext.getBean(PublisherInvalidExchange)
        PollingConditions conditions = new PollingConditions(delay: 3, timeout: 7)

        when:
        Throwable t
        publisher.invalidExchange("abc")
            .subscribe(() -> {}, (ex) -> {
                t = ex
            })

        then:
        conditions.eventually {
            t instanceof RabbitClientException
            t.message.contains("Timed out waiting for publisher confirm")
        }
    }

    @Requires(property = "spec.name", value = "ClientErrorSpec")
    @RabbitClient
    static interface Publisher {

        void noBody(@Header String contentType)

        void invalidProperty(@RabbitProperty String xyz, byte[] body)

        void invalidBody(TimeZone body)

    }

    @Requires(property = "spec.name", value = "ClientErrorSpec")
    @RabbitClient("abc-xyz")
    static interface PublisherInvalidExchange {

        Completable invalidExchange(@Body String body)
    }
}
