package io.micronaut.rabbitmq.docs.consumer.executor;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
class CustomExecutorSpec implements TestPropertyProvider {

    @Test
    void testProductClientAndListener(ProductClient productClient, ProductListener productListener) {

// tag::producer[]
productClient.send("custom-executor-test".getBytes());
// end::producer[]

        await().atMost(60, SECONDS).until(() ->
                productListener.messageLengths.size() == 1 &&
                productListener.messageLengths.get(0).equals("custom-executor-test")
        );
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> config = new HashMap<>();
        config.put("micronaut.executors.product-listener.type", "FIXED");
        return config;
    }
}
