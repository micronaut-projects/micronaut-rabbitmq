package io.micronaut.rabbitmq.docs.consumer.types

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await


@MicronautTest
@Property(name = "spec.name", value = "TypeBindingSpec")
class TypeBindingSpec extends Specification {
    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test publishing and receiving rabbitmq types"() {
        when:
// tag::producer[]
        productClient.send("body".bytes, "text/html")
        productClient.send("body2".bytes, "application/json")
        productClient.send("body3".bytes, "text/xml")
// end::producer[]

        await().atMost(10, SECONDS).until {
            productListener.messages.size() == 3
        }

        then:
        productListener.messages.size() == 3
        productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/html]")
        productListener.messages.contains("exchange: [], routingKey: [product], contentType: [application/json]")
        productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/xml]")
    }
}
