package io.micronaut.rabbitmq.docs.rpc

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import spock.lang.Specification
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "RpcUppercaseSpec")
class RpcUppercaseSpec extends Specification implements TestPropertyProvider {
    @Override
    Map<String, String> getProperties() {
        RabbitMQ.getProperties();
    }

    @Inject ProductClient productClient

    void "test product client and listener"() {
// tag::producer[]
        expect:
        productClient.send("hello") == "HELLO"
        Mono.from(productClient.sendReactive("world")).block() == "WORLD"
// end::producer[]
    }
}
