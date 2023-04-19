package io.micronaut.rabbitmq.docs.consumer.types;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
class TypeBindingSpec {

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
