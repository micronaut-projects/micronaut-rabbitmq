package io.micronaut.rabbitmq.docs.consumer.connection;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
class ConnectionSpec {

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
// TODO
//    @Override
//    public Map<String, String> getProperties() {
//        Map<String, String> config = new HashMap<>();
//        config.put("rabbitmq.servers.product-cluster.port", config.remove("rabbitmq.port"));
//        return config;
//    }
}
