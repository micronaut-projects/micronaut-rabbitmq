package io.micronaut.rabbitmq.docs.rpc;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

public class RpcUppercaseSpec extends AbstractRabbitMQTest {

    @Test
    void testProductClientAndListener() {
        ApplicationContext applicationContext = startContext();

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
assert productClient.send("rpc").equals("RPC");
assert productClient.sendReactive("hello").blockingGet().equals("HELLO");
// end::producer[]

        applicationContext.close();
    }
}
