package io.micronaut.rabbitmq.docs.serdes;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

@MicronautTest
@Property(name = "spec.name", value = "ProductInfoSerDesSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductInfoSerDesSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testUsingACustomSerDes(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
        productClient.send(new ProductInfo("small", 10L, true));
        productClient.send(new ProductInfo("medium", 20L, true));
        productClient.send(new ProductInfo(null, 30L, false));
// end::producer[]


        await().atMost(60, SECONDS).until(() ->
                productListener.messages.size() == 3 &&
                productListener.messages.stream().anyMatch(pi -> pi.getCount() == 10L) &&
                productListener.messages.stream().anyMatch(pi -> pi.getCount() == 20L) &&
                productListener.messages.stream().anyMatch(pi -> pi.getCount() == 30L)
        );
    }
}
