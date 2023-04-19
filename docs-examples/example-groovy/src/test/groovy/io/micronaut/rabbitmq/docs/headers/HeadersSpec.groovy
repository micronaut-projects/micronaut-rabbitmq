package io.micronaut.rabbitmq.docs.headers

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await


@MicronautTest
@Property(name = "spec.name", value = "HeadersSpec")
class HeadersSpec extends Specification {
    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test publishing and receiving headers"() {
        when:
// tag::producer[]
        productClient.send("body".bytes)
        productClient.send("medium", 20L, "body2".bytes)
        productClient.send(null, 30L, "body3".bytes)
        productClient.send([productSize: "large", "x-product-count": 40L], "body4".bytes)
// end::producer[]

        await().atMost(10, SECONDS).until {
            productListener.messageProperties.size() == 4
        }

        then:
        productListener.messageProperties.size() == 4
        productListener.messageProperties.contains("true|10|small")
        productListener.messageProperties.contains("true|20|medium")
        productListener.messageProperties.contains("true|30|null")
        productListener.messageProperties.contains("true|40|large")
    }
}
