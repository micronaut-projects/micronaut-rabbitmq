package io.micronaut.rabbitmq.docs.quickstart;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;
import org.junit.jupiter.api.TestInstance;

@MicronautTest
@Property(name = "spec.name", value = "QuickstartSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuickstartSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testProductClientAndListener(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
productClient.send("quickstart".getBytes());
// end::producer[]

        await().atMost(60, SECONDS).until(() ->
                productListener.messageLengths.size() == 1 &&
                productListener.messageLengths.get(0).equals("quickstart")
        );
    }
}
