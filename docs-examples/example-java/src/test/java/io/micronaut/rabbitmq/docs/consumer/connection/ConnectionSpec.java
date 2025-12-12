package io.micronaut.rabbitmq.docs.consumer.connection;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.micronaut.context.annotation.Property;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
@Property(name = "spec.name", value = "ConnectionSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionSpec implements TestPropertyProvider {

    @Test
    void testProductClientAndListener(ProductClient productClient, ProductListener productListener) {

// tag::producer[]
productClient.send("connection-test".getBytes());
// end::producer[]

        await().atMost(10, SECONDS).until(() ->
                productListener.messageLengths.size() == 1 &&
                productListener.messageLengths.get(0).equals("connection-test")
        );
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
