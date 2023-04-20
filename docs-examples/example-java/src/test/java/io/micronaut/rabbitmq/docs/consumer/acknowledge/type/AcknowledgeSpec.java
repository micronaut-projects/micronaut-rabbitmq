package io.micronaut.rabbitmq.docs.consumer.acknowledge.type;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
@Property(name = "spec.name", value = "AcknowledgeSpec")
class AcknowledgeSpec {

    @Test
    void testAckingWithAcknowledgement(ProductClient productClient, ProductListener productListener) {

// tag::producer[]
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
productClient.send("message body".getBytes());
// end::producer[]

        await().atMost(60, SECONDS).until(() ->
                productListener.messageCount.get() == 5 // the first message is rejected and re-queued
        );
    }
}
