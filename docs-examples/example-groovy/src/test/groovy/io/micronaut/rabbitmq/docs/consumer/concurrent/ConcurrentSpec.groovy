package io.micronaut.rabbitmq.docs.consumer.concurrent

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "ConcurrentSpec")
class ConcurrentSpec extends Specification implements TestPropertyProvider {
    @Override
    Map<String, String> getProperties() {
        RabbitMQ.getProperties();
    }
    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test concurrent consumers"() {

        when:
        4.times { productClient.send("body".bytes) }

        await().atMost(10, SECONDS).until {
            productListener.threads.size() == 4
        }

        then:
        productListener.threads.size() == 4
    }
}
