package io.micronaut.rabbitmq.docs.quickstart;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class QuickstartSpec extends AbstractRabbitMQTest {

    @Test
    void testProductClientAndListener() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
productClient.send("quickstart".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.messageLengths.size() == 1 &&
                            productListener.messageLengths.get(0).equals("quickstart")
            );
        } finally {
            applicationContext.close();
        }
    }
}
