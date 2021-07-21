package io.micronaut.rabbitmq.rpc

import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import reactor.core.publisher.Mono

class RpcSpec extends AbstractRabbitMQTest {

    void "test simple RPC call"() {
        ApplicationContext applicationContext = startContext()
        RpcPublisher producer = applicationContext.getBean(RpcPublisher)

        expect:
        Mono.from(producer.rpcCall("hello")).block() == "HELLO"
        producer.rpcBlocking("world") == "WORLD"

        cleanup:
        applicationContext.close()
    }
}
