package io.micronaut.rabbitmq.docs.consumer.custom.type;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
@Property(name = "spec.name", value = "ProductInfoSpec")
class ProductInfoSpec {

    @Test
    void testUsingACustomTypeBinder(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
        productClient.send("body".getBytes());
        productClient.send("medium", 20L, "body2".getBytes());
        productClient.send(null, 30L, "body3".getBytes());
// end::producer[]


        await().atMost(60, SECONDS).until(() ->
                productListener.messages.size() == 3 &&
                productListener.messages.stream().anyMatch(pi -> pi.getCount() == 10L) &&
                productListener.messages.stream().anyMatch(pi -> pi.getCount() == 20L) &&
                productListener.messages.stream().anyMatch(pi -> pi.getCount() == 30L)
        );
    }
}
