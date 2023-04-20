package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await


@MicronautTest
@Property(name = "spec.name", value = "ProductInfoSpec")
class ProductInfoSpec extends Specification {
    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test using a custom type binder"() {
        when:
// tag::producer[]
        productClient.send("body".bytes)
        productClient.send("medium", 20L, "body2".bytes)
        productClient.send(null, 30L, "body3".bytes)
// end::producer[]


        await().atMost(10, SECONDS).until {
            productListener.messages.size() == 3
        }

        then:
        productListener.messages.size() == 3

        productListener.messages.find({ pi ->
            pi.size == "small" && pi.count == 10 && pi.sealed
        }) != null

        productListener.messages.find({ pi ->
            pi.size == "medium" && pi.count == 20 && pi.sealed
        }) != null

        productListener.messages.find({ pi ->
            pi.size == null && pi.count == 30 && pi.sealed
        }) != null
    }
}
