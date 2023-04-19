package io.micronaut.rabbitmq.docs.consumer.acknowledge.type

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await

@MicronautTest
@Property(name = "spec.name", value = "AcknowledgeSpec")
class AcknowledgeSpec extends Specification {

    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test acking with an acknowledgement argument"() {

        when:
// tag::producer[]
productClient.send("message body".bytes)
productClient.send("message body".bytes)
productClient.send("message body".bytes)
productClient.send("message body".bytes)
// end::producer[]

        await().atMost(10, SECONDS).until {
            productListener.messageCount.get() == 5
        }

        then:
        productListener.messageCount.get() == 5
    }
}
