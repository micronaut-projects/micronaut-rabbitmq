package io.micronaut.rabbitmq.docs.consumer.connection

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import spock.lang.Specification
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await


@MicronautTest(rebuildContext = true)
@Property(name = "spec.name", value = "ConnectionSpec")
class ConnectionSpec extends Specification implements TestPropertyProvider {
    @Inject ProductClient productClient
    @Inject ProductListener productListener

    void "test product client and listener"() {

        when:
// tag::producer[]
        productClient.send("connection-test".bytes)
// end::producer[]

        await().atMost(10, SECONDS).until {
            productListener.messageLengths.size() == 1 &&
                    productListener.messageLengths[0] == "connection-test"
        }

        then:
        productListener.messageLengths.size() == 1
        productListener.messageLengths[0] == "connection-test"

        cleanup:
        // Finding that the context is closing the channel before ack is sent
        sleep 200
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> m = new HashMap<>(RabbitMQ.getProperties());
        String uri = m.get("rabbitmq.uri");
        String port = String.valueOf(URI.create(uri).getPort());
        m.put("rabbitmq.servers.product-cluster.port", port);
        return m;
    }
}
