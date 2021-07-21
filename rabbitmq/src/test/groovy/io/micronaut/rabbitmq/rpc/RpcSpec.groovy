package io.micronaut.rabbitmq.rpc

import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class RpcSpec extends AbstractRabbitMQTest {

    void "test simple RPC call"() {
        ApplicationContext applicationContext = startContext()
        RpcPublisher producer = applicationContext.getBean(RpcPublisher)

        expect:
        producer.rpcCall("hello").blockingFirst() == "HELLO"
        producer.rpcCallMono("hello").blockingGet() == "HELLO"
        producer.rpcCallMono(null).blockingGet() == null
        producer.rpcCallSingle("hello").blockingGet() == "HELLO"
        producer.rpcBlocking("world") == "WORLD"

        when:
        producer.rpcCallSingle(null).blockingGet() == null

        then:
        thrown(NoSuchElementException)

        cleanup:
        applicationContext.close()
    }
}
