package io.micronaut.rabbitmq.docs.consumer.custom.annotation;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

public class DeliveryTagSpec extends AbstractRabbitMQTest {

    @Test
    void testUsingACustomAnnotationBinder() {
        startContext();

// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        productClient.send("body".getBytes());
        productClient.send("body2".getBytes());
        productClient.send("body3".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        waitFor(() -> productListener.messages.size() == 3);
    }
}
