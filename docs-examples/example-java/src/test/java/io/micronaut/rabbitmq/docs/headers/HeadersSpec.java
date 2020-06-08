package io.micronaut.rabbitmq.docs.headers;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class HeadersSpec extends AbstractRabbitMQTest {

    @Test
    void testPublishingAndReceivingHeaders() {
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
                    productListener.messageProperties.size() == 3 &&
                            productListener.messageProperties.contains("true|10|small") &&
                            productListener.messageProperties.contains("true|20|medium") &&
                            productListener.messageProperties.contains("true|30|null")
            );
        } finally {
            applicationContext.close();
        }
    }
}
