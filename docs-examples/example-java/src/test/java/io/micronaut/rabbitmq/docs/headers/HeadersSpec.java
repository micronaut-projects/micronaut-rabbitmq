package io.micronaut.rabbitmq.docs.headers;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
@Property(name = "spec.name", value = "HeadersSpec")
class HeadersSpec {

    @Test
    void testPublishingAndReceivingHeaders(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
        productClient.send("body".getBytes());
        productClient.send("medium", 20L, "body2".getBytes());
        productClient.send(null, 30L, "body3".getBytes());

        Map<String, Object> headers = new HashMap<>(3);
        headers.put("productSize", "large");
        headers.put("x-product-count", 40L);
        productClient.send(headers, "body4".getBytes());
// end::producer[]

        await().atMost(60, SECONDS).until(() ->
                productListener.messageProperties.size() == 4 &&
                productListener.messageProperties.contains("true|10|small") &&
                productListener.messageProperties.contains("true|20|medium") &&
                productListener.messageProperties.contains("true|30|null") &&
                productListener.messageProperties.contains("true|40|large")
        );
    }
}
