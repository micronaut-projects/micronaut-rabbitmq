package io.micronaut.rabbitmq.docs.consumer.concurrent;

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
@Property(name = "spec.name", value = "ConcurrentSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrentSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testConcurrentConsumers(ProductClient productClient, ProductListener productListener) {

        for (int i = 0; i < 4; i++) {
            productClient.send("body".getBytes());
        }


        await().atMost(60, SECONDS).until(() -> productListener.threads.size() == 4);
    }
}
