package io.micronaut.rabbitmq.docs.parameters;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
@Property(name = "spec.name", value = "BindingSpec")
class BindingSpec {

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
