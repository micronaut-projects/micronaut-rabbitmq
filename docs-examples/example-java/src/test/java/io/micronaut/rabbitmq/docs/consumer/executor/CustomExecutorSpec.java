package io.micronaut.rabbitmq.docs.consumer.executor;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

class CustomExecutorSpec extends AbstractRabbitMQTest {

    @Test
    void testProductClientAndListener() {
        startContext();

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
productClient.send("custom-executor-test".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        waitFor(() ->
                productListener.messageLengths.size() == 1 &&
                productListener.messageLengths.get(0).equals("custom-executor-test")
        );
    }

    protected Map<String, Object> getConfiguration() {
        Map<String, Object> config = super.getConfiguration();
        config.put("micronaut.executors.product-listener.type", "FIXED");
        return config;
    }
}
