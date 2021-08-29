package io.micronaut.rabbitmq.docs.consumer.connection;

import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class ConnectionSpec extends AbstractRabbitMQTest {

    @Test
    void testProductClientAndListener() {
        startContext();

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
productClient.send("connection-test".getBytes());
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener.class);

        waitFor(() ->
                productListener.messageLengths.size() == 1 &&
                productListener.messageLengths.get(0).equals("connection-test")
        );
    }

    protected Map<String, Object> getConfiguration() {
        Map<String, Object> config = super.getConfiguration();
        config.put("rabbitmq.servers.product-cluster.port", config.remove("rabbitmq.port"));
        return config;
    }
}
