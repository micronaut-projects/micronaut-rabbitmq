package io.micronaut.rabbitmq.docs.consumer.executor;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest(rebuildContext = true)
@Property(name = "spec.name", value = "CustomExecutorSpec")
@Property(name = "micronaut.executors.product-listener.type", value = "FIXED")
class CustomExecutorSpec {

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
}
