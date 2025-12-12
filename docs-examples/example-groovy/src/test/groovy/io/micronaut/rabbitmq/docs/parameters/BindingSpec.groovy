package io.micronaut.rabbitmq.docs.parameters

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "BindingSpec")
class BindingSpec extends Specification implements TestPropertyProvider {
    @Override
    Map<String, String> getProperties() {
        RabbitMQ.getProperties();
    }

    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test dynamic binding"() {
        when:
// tag::producer[]
        productClient.send("message body".bytes)
        productClient.send("product", "message body2".bytes)
// end::producer[]

        await().atMost(10, SECONDS).until {
            productListener.messageLengths.size() == 2
        }
        then:
        productListener.messageLengths.size() == 2
        productListener.messageLengths.contains(12)
        productListener.messageLengths.contains(13)
    }
}
