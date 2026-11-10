package io.micronaut.rabbitmq.rpc

import io.micronaut.rabbitmq.AbstractRabbitMQTest
import reactor.core.publisher.Mono

class RpcSpec extends AbstractRabbitMQTest {

    void "test simple RPC call"() {
        startContext()

        RpcPublisher producer = applicationContext.getBean(RpcPublisher)

        expect:
        Mono.from(producer.rpcCall("hello")).block() == "HELLO"
        producer.rpcBlocking("world") == "WORLD"
    }

    void "test RPC call with CompletableFuture return type"() {
        startContext()

        RpcPublisher producer = applicationContext.getBean(RpcPublisher)

        expect:
        producer.rpcFuture("hello").get() == "HELLO"
    }

    void "test RPC call with CompletionStage return type"() {
        startContext()

        RpcPublisher producer = applicationContext.getBean(RpcPublisher)

        expect:
        producer.rpcStage("world").toCompletableFuture().get() == "WORLD"
    }
}
