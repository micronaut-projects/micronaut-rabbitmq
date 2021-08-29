package io.micronaut.rabbitmq.docs.quickstart;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

public class QuickstartSpec extends AbstractRabbitMQTest {

    @Test
    void testProductClientAndListener() {
        startContext();

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
productClient.send("quickstart".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        waitFor(() ->
                productListener.messageLengths.size() == 1 &&
                productListener.messageLengths.get(0).equals("quickstart")
        );
    }
}
