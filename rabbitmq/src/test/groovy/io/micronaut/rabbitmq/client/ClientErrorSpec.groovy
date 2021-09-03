package io.micronaut.rabbitmq.client

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitProperty
import io.micronaut.rabbitmq.exception.RabbitClientException
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.util.concurrent.PollingConditions

class ClientErrorSpec extends AbstractRabbitMQTest {

    void "test no body argument"() {
        startContext()

        Sender sender = applicationContext.getBean(Sender)

        when:
        sender.noBody("abc")

        then:
        RabbitClientException ex = thrown()
        ex.message.startsWith("No valid message body argument found")

        when:
        sender.invalidProperty("abc")

        then:
        ex = thrown()
        ex.message == "Attempted to set property [xyz], but could not match the name to any of the com.rabbitmq.client.BasicProperties"

        when:
        sender.invalidBody(TimeZone.getDefault())

        then:
        ex = thrown()
        ex.message == "Could not find a serializer for the body argument of type [java.util.TimeZone]"
    }

    void "test publishing to an exchange that does not exist"() {
        startContext()

        SenderInvalidExchange sender = applicationContext.getBean(SenderInvalidExchange)
        PollingConditions conditions = new PollingConditions(delay: 3, timeout: 7)

        when:
        Throwable abc
        sender.invalidExchange("abc").subscribe(new Subscriber<Void>() {

            Subscription s

            @Override
            void onSubscribe(Subscription s) {
                this.s = s
                s.request(1)
            }

            @Override
            void onError(Throwable t) {
                abc = t
            }

            @Override void onNext(Void unused) {}
            @Override void onComplete() {}
        })

        then:
        conditions.eventually {
            abc instanceof RabbitClientException
            abc.message.contains("Timed out waiting for publisher confirm")
        }
    }

    @Requires(property = "spec.name", value = "ClientErrorSpec")
    @RabbitClient
    static interface Sender {
        void noBody(@MessageHeader String contentType)
        void invalidProperty(@RabbitProperty String xyz, byte[] body)
        void invalidBody(TimeZone body)
    }

    @Requires(property = "spec.name", value = "ClientErrorSpec")
    @RabbitClient("abc-xyz")
    static interface SenderInvalidExchange {
        Publisher<Void> invalidExchange(@MessageBody String body)
    }
}
