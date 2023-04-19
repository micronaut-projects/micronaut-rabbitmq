package io.micronaut.rabbitmq.docs.consumer.concurrent;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
class ConcurrentSpec {

    @Test
    void testConcurrentConsumers(ProductClient productClient, ProductListener productListener) {

        for (int i = 0; i < 4; i++) {
            productClient.send("body".getBytes());
        }


        await().atMost(60, SECONDS).until(() -> productListener.threads.size() == 4);
    }
}
