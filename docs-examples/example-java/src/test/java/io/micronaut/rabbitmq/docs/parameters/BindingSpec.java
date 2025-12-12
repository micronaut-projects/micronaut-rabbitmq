package io.micronaut.rabbitmq.docs.parameters;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
@Property(name = "spec.name", value = "BindingSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BindingSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testDynamicBinding(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
        productClient.send("message body".getBytes());
        productClient.send("product", "message body2".getBytes());
// end::producer[]


        await().atMost(60, SECONDS).until(() ->
                productListener.messageLengths.size() == 2 &&
                productListener.messageLengths.contains(12) &&
                productListener.messageLengths.contains(13)
        );
    }
}
