package io.micronaut.rabbitmq.docs.consumer.custom.annotation

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "DeliveryTagSpec")
class DeliveryTagSpec extends Specification implements TestPropertyProvider {
    @Override
    Map<String, String> getProperties() {
        RabbitMQ.getProperties();
    }

    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test using a custom annotation binder"() {

        when:
// tag::producer[]
        productClient.send("body".bytes)
        productClient.send("body2".bytes)
        productClient.send("body3".bytes)
// end::producer[]
        await().atMost(10, SECONDS).until {
            productListener.messages.size() == 3
        }

        then:
        productListener.messages.size() == 3
    }
}
