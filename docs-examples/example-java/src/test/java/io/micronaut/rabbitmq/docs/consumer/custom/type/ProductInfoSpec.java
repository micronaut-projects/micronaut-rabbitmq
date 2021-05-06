package io.micronaut.rabbitmq.docs.consumer.custom.type;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class ProductInfoSpec extends AbstractRabbitMQTest {

    @Test
    void testUsingACustomTypeBinder() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        productClient.send("body".getBytes());
        productClient.send("medium", 20L, "body2".getBytes());
        productClient.send(null, 30L, "body3".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.messages.size() == 3 &&
                    productListener.messages.stream().anyMatch(pi -> pi.getCount() == 10L) &&
                    productListener.messages.stream().anyMatch(pi -> pi.getCount() == 20L) &&
                    productListener.messages.stream().anyMatch(pi -> pi.getCount() == 30L)
            );
        } finally {
            applicationContext.close();
        }
    }
}
