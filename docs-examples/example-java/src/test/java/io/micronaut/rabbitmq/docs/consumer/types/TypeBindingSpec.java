package io.micronaut.rabbitmq.docs.consumer.types;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class TypeBindingSpec extends AbstractRabbitMQTest {

    @Test
    void testBindingByType() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        productClient.send("body".getBytes(), "text/html");
        productClient.send("body2".getBytes(), "application/json");
        productClient.send("body3".getBytes(), "text/xml");
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.messages.size() == 3 &&
                    productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/html]") &&
                    productListener.messages.contains("exchange: [], routingKey: [product], contentType: [application/json]") &&
                    productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/xml]")
            );
        } finally {
            applicationContext.close();
        }
    }
}
