package io.micronaut.rabbitmq.docs.quickstart;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@MicronautTest
public class QuickstartSpec {

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
