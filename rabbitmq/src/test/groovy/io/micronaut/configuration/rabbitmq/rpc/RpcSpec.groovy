package io.micronaut.configuration.rabbitmq.rpc

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext

class RpcSpec extends AbstractRabbitMQTest {

    void "test simple RPC call"() {
        ApplicationContext applicationContext = startContext()
        RpcPublisher producer = applicationContext.getBean(RpcPublisher)

        expect:
        producer.rpcCall("hello").blockingFirst() == "HELLO"
        producer.rpcBlocking("world") == "WORLD"

        cleanup:
        applicationContext.close()
    }
}
