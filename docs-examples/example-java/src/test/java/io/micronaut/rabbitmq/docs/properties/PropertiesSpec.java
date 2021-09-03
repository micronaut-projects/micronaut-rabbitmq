package io.micronaut.rabbitmq.docs.properties;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

public class PropertiesSpec extends AbstractRabbitMQTest {

    @Test
    void testPublishingAndReceivingProperties() {
        startContext();

// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        productClient.send("body".getBytes());
        productClient.send("guest", "text/html", "body2".getBytes());
        productClient.send("guest", null, "body3".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        waitFor(() ->
                productListener.messageProperties.size() == 3 &&
                productListener.messageProperties.contains("guest|application/json|myApp") &&
                productListener.messageProperties.contains("guest|text/html|myApp") &&
                productListener.messageProperties.contains("guest|null|myApp")
        );
    }
}
