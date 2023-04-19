package io.micronaut.rabbitmq.docs.rpc;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class RpcUppercaseSpec {

    @Test
    void testProductClientAndListener(ProductClient productClient) {
// tag::producer[]
assertEquals("RPC", productClient.send("rpc"));
assertEquals("HELLO", Mono.from(productClient.sendReactive("hello")).block());
// end::producer[]
    }
}
