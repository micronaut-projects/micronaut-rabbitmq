package io.micronaut.rabbitmq.docs.consumer.concurrent;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class ConcurrentSpec extends AbstractRabbitMQTest {

    @Test
    void testConcurrentConsumers() {
        ApplicationContext applicationContext = startContext();

        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        for (int i = 0; i < 4; i++) { productClient.send("body".getBytes()); }

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.threads.size() == 4
            );
        } finally {
            applicationContext.close();
        }
    }
}
