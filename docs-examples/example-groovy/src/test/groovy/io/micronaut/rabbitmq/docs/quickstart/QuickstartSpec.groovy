package io.micronaut.rabbitmq.docs.quickstart

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await


@MicronautTest
@Property(name = "spec.name", value = "QuickstartSpec")
class QuickstartSpec extends Specification {
    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test product client and listener"() {
        when:
// tag::producer[]
productClient.send("quickstart".bytes)
// end::producer[]

        await().atMost(10, SECONDS).until {
            productListener.messageLengths.size() == 1
        }

        then:
        productListener.messageLengths.size() == 1
        productListener.messageLengths[0] == "quickstart"

        cleanup:
        // Finding that the context is closing the channel before ack is sent
        sleep 200
    }
}
