package io.micronaut.rabbitmq.docs.consumer.custom.annotation;

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
@Property(name = "spec.name", value = "DeliveryTagSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeliveryTagSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testUsingACustomAnnotationBinder(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
        productClient.send("body".getBytes());
        productClient.send("body2".getBytes());
        productClient.send("body3".getBytes());
// end::producer[]


        await().atMost(60, SECONDS).until(() -> productListener.messages.size() == 3);
    }
}
