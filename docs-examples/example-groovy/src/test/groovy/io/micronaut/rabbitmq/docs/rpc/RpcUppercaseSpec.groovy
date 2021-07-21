package io.micronaut.rabbitmq.docs.rpc

import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import reactor.core.publisher.Mono

class RpcUppercaseSpec extends AbstractRabbitMQTest {

    void "test product client and listener"() {
        ApplicationContext applicationContext = startContext()

        when:
// tag::producer[]
        def productClient = applicationContext.getBean(ProductClient)

        then:
        productClient.send("hello") == "HELLO"
        Mono.from(productClient.sendReactive("world")).block() == "WORLD"
// end::producer[]

        cleanup:
        applicationContext.close()
    }
}
