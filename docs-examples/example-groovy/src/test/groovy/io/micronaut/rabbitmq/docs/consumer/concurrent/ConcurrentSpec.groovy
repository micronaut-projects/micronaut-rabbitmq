package io.micronaut.rabbitmq.docs.consumer.concurrent

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await


@MicronautTest
@Property(name = "spec.name", value = "ConcurrentSpec")
class ConcurrentSpec extends Specification {
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
