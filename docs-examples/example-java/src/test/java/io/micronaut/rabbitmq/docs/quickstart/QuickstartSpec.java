package io.micronaut.rabbitmq.docs.quickstart;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
@Property(name = "spec.name", value = "QuickstartSpec")
class QuickstartSpec {

    @Test
    void testProductClientAndListener(ProductClient productClient, ProductListener productListener) {
// tag::producer[]
productClient.send("quickstart".getBytes());
// end::producer[]

        await().atMost(60, SECONDS).until(() ->
                productListener.messageLengths.size() == 1 &&
                productListener.messageLengths.get(0).equals("quickstart")
        );
    }
}
