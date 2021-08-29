package io.micronaut.rabbitmq.docs.consumer.acknowledge.type;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

public class AcknowledgeSpec extends AbstractRabbitMQTest {

    @Test
    void testAckingWithAcknowledgement() {
        startContext();

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        waitFor(() ->
                productListener.messageCount.get() == 5 // the first message is rejected and re-queued
        );
    }
}
