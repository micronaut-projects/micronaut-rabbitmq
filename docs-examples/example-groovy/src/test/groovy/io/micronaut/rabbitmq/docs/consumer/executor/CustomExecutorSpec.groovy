package io.micronaut.rabbitmq.docs.consumer.executor

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await

@MicronautTest(rebuildContext = true)
@Property(name = "spec.name", value = "CustomExecutorSpec")
@Property(name = "micronaut.executors.product-listener.type", value = "FIXED")
class CustomExecutorSpec extends Specification {

    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test product client and listener"() {
        when:
// tag::producer[]
        productClient.send("custom-executor-test".bytes)
// end::producer[]

        await().atMost(10, SECONDS).until {
            productListener.messageLengths.size() == 1
        }

        then:
        productListener.messageLengths.size() == 1
        productListener.messageLengths[0] == "custom-executor-test"
    }
}
