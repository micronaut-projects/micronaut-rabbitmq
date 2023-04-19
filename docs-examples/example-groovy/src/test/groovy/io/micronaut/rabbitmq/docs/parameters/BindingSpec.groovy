package io.micronaut.rabbitmq.docs.parameters

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await


@MicronautTest
@Property(name = "spec.name", value = "BindingSpec")
class BindingSpec extends Specification {
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
