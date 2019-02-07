package io.micronaut.configuration.rabbitmq.docs.consumer.acknowledge.bool;

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class BooleanAckSpec extends AbstractRabbitMQTest {

    @Test
    void testAckingWithBoolean() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
productClient.send("message body".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.messageCount.get() == 2
            );
        } finally {
            applicationContext.close();
        }
    }
}
