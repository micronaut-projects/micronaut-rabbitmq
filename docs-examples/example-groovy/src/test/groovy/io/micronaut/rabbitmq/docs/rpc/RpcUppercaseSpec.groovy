package io.micronaut.rabbitmq.docs.rpc

import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext

class RpcUppercaseSpec extends AbstractRabbitMQTest {

    void "test product client and listener"() {
        ApplicationContext applicationContext = startContext()

        when:
// tag::producer[]
        def productClient = applicationContext.getBean(ProductClient)

        then:
        productClient.send("hello") == "HELLO"
        productClient.sendReactive("world").blockingGet() == "WORLD"
// end::producer[]

        cleanup:
        applicationContext.close()
    }
}