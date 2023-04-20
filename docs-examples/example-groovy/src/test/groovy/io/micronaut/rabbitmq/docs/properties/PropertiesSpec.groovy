package io.micronaut.rabbitmq.docs.properties

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await

@MicronautTest
@Property(name = "spec.name", value = "PropertiesSpec")
class PropertiesSpec extends Specification {
    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test sending and receiving properties"() {
        when:
// tag::producer[]
        productClient.send("body".bytes)
        productClient.send("guest", "text/html", "body2".bytes)
        productClient.send("guest", null, "body3".bytes)
// end::producer[]

        await().atMost(10, SECONDS).until {
            productListener.messageProperties.size() == 3
        }

        then:
        productListener.messageProperties.contains("guest|application/json|myApp")
        productListener.messageProperties.contains("guest|text/html|myApp")
        productListener.messageProperties.contains("guest|null|myApp")
    }
}
