package io.micronaut.rabbitmq.docs.headers

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class HeadersSpec extends AbstractRabbitMQTest {

    void "test publishing and receiving headers"() {
        when:
        startContext()
// tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        productClient.send("body".bytes)
        productClient.send("medium", 20L, "body2".bytes)
        productClient.send(null, 30L, "body3".bytes)
        productClient.send([productSize: "large", "x-product-count": 40L], "body4".bytes)
// end::producer[]

        ProductListener productListener = applicationContext.getBean(ProductListener)

        then:
        waitFor {
            assert productListener.messageProperties.size() == 4
            assert productListener.messageProperties.contains("true|10|small")
            assert productListener.messageProperties.contains("true|20|medium")
            assert productListener.messageProperties.contains("true|30|null")
            assert productListener.messageProperties.contains("true|40|large")
        }
    }
}
