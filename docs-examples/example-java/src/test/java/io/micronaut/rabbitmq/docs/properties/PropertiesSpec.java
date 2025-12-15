package io.micronaut.rabbitmq.docs.properties;

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
@Property(name = "spec.name", value = "PropertiesSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertiesSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testPublishingAndReceivingProperties(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
        productClient.send("body".getBytes());
        productClient.send("guest", "text/html", "body2".getBytes());
        productClient.send("guest", null, "body3".getBytes());
// end::producer[]


        await().atMost(60, SECONDS).until(() ->
                productListener.messageProperties.size() == 3 &&
                productListener.messageProperties.contains("guest|application/json|myApp") &&
                productListener.messageProperties.contains("guest|text/html|myApp") &&
                productListener.messageProperties.contains("guest|null|myApp")
        );
    }
}
