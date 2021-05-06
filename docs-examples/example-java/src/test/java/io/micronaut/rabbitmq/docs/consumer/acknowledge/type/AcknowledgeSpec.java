package io.micronaut.rabbitmq.docs.consumer.acknowledge.type;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class AcknowledgeSpec extends AbstractRabbitMQTest {

    @Test
    void testAckingWithAcknowledgement() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.messageCount.get() == 5 // the first message is rejected and re-queued
            );
        } finally {
            applicationContext.close();
        }
    }
}
