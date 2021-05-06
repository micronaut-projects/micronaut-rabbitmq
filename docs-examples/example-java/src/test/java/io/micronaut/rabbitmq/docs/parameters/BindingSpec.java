package io.micronaut.rabbitmq.docs.parameters;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class BindingSpec extends AbstractRabbitMQTest {

    @Test
    void testDynamicBinding() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient.class);
        productClient.send("message body".getBytes());
        productClient.send("product", "message body2".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        try {
            await().atMost(5, SECONDS).until(() ->
                    productListener.messageLengths.size() == 2 &&
                            productListener.messageLengths.contains(12) &&
                            productListener.messageLengths.contains(13)
            );
        } finally {
            applicationContext.close();
        }
    }
}
