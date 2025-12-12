package io.micronaut.rabbitmq.docs.rpc;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;

@MicronautTest
@Property(name = "spec.name", value = "RpcUppercaseSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RpcUppercaseSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testProductClientAndListener(ProductClient productClient) {
// tag::producer[]
assertEquals("RPC", productClient.send("rpc"));
assertEquals("HELLO", Mono.from(productClient.sendReactive("hello")).block());
// end::producer[]
    }
}
