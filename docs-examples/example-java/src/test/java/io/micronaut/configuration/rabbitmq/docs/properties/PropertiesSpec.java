package io.micronaut.configuration.rabbitmq.docs.properties;

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class PropertiesSpec extends AbstractRabbitMQTest {

    @Test
    void testPublishingAndReceivingProperties() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        productClient.send("body".getBytes());
        productClient.send("guest", "text/html", "body2".getBytes());
        productClient.send("guest", null, "body3".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.messageProperties.size() == 3 &&
                    productListener.messageProperties.contains("guest|application/json|myApp") &&
                    productListener.messageProperties.contains("guest|text/html|myApp") &&
                    productListener.messageProperties.contains("guest|null|myApp")
            );
        } finally {
            applicationContext.close();
        }
    }
}
