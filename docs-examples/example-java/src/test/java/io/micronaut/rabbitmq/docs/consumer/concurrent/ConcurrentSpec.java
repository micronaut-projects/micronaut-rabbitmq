package io.micronaut.rabbitmq.docs.consumer.concurrent;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

public class ConcurrentSpec extends AbstractRabbitMQTest {

    @Test
    void testConcurrentConsumers() {
        startContext();

        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        for (int i = 0; i < 4; i++) {
            productClient.send("body".getBytes());
        }

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        waitFor(() -> productListener.threads.size() == 4);
    }
}
