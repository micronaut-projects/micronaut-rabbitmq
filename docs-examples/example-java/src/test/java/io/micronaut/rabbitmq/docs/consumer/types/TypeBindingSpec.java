package io.micronaut.rabbitmq.docs.consumer.types;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;

@MicronautTest
@Property(name = "spec.name", value = "TypeBindingSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeBindingSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testBindingByType(ProductClient productClient, ProductListener productListener) {

// tag::producer[]
        productClient.send("body".getBytes(), "text/html");
        productClient.send("body2".getBytes(), "application/json");
        productClient.send("body3".getBytes(), "text/xml");
// end::producer[]


        await().atMost(60, SECONDS).until(() ->
                productListener.messages.size() == 3 &&
                productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/html]") &&
                productListener.messages.contains("exchange: [], routingKey: [product], contentType: [application/json]") &&
                productListener.messages.contains("exchange: [], routingKey: [product], contentType: [text/xml]")
        );
    }
}
