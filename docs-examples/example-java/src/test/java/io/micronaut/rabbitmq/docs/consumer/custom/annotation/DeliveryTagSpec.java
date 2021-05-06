package io.micronaut.rabbitmq.docs.consumer.custom.annotation;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class DeliveryTagSpec extends AbstractRabbitMQTest {

    @Test
    void testUsingACustomAnnotationBinder() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        productClient.send("body".getBytes());
        productClient.send("body2".getBytes());
        productClient.send("body3".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.messages.size() == 3
            );
        } finally {
            applicationContext.close();
        }
    }
}
