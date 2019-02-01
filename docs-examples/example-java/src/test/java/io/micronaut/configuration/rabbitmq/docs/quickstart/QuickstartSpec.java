package io.micronaut.configuration.rabbitmq.docs.quickstart;

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class QuickstartSpec extends AbstractRabbitMQTest {

    @Test
    void testProductClientAndListener() {
        Map<String, Object> config = new HashMap<>();
        config.put("rabbitmq.port", rabbitContainer.getMappedPort(5672));
        config.put("spec.name", this.getClass().getSimpleName());
        ApplicationContext applicationContext = ApplicationContext.run(config, "test");

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
productClient.send("message body".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        await().atMost(5, SECONDS).until(() ->
                productListener.messageLengths.size() == 1 &&
            productListener.messageLengths.get(0) == 12
        );

        applicationContext.close();
    }
}
