package io.micronaut.rabbitmq.rpc

import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.serde.annotation.Serdeable
import reactor.core.publisher.Mono
import spock.lang.Timeout

import java.time.Duration
import java.util.concurrent.TimeUnit

class RpcSpec extends AbstractRabbitMQTest {

    void "test simple RPC call"() {
        startContext()

        RpcPublisher producer = applicationContext.getBean(RpcPublisher)

        expect:
        Mono.from(producer.rpcCall("hello")).block() == "HELLO"
        producer.rpcBlocking("world") == "WORLD"
    }

    @Timeout(15)
    void "test RPC #method preserves generic reply type on repeated calls"() {
        startContext()

        RpcPublisher producer = applicationContext.getBean(RpcPublisher)

        expect:
        Mono.from(producer.rpcList("first")).block(Duration.ofSeconds(5))*.name == ["first", "second"]
        producer.rpcListBlocking("first")*.name == ["first", "second"]

        when:
        List<Person> first = producer."$method"("first").toCompletableFuture().get(5, TimeUnit.SECONDS)
        List<Person> second = producer."$method"("again").toCompletableFuture().get(5, TimeUnit.SECONDS)

        then:
        first.every { it instanceof Person }
        first*.name == ["first", "second"]
        second.every { it instanceof Person }
        second*.name == ["again", "second"]

        where:
        method << ["rpcListAsync", "rpcListFuture"]
    }

    @Serdeable
    static class Person {
        String name
    }
}
