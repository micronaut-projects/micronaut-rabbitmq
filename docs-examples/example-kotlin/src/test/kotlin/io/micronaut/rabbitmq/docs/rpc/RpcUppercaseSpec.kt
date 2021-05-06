package io.micronaut.rabbitmq.docs.rpc

import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class RpcUppercaseSpec: AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = startContext(specName)

        `when`("the message is published") {

            val productClient = ctx.getBean(ProductClient::class.java)

            then("the message is consumed") {
                productClient.send("hello") shouldBe "HELLO"
                productClient.sendReactive("world").blockingGet() shouldBe "WORLD"
            }
        }

        ctx.stop()
    }

})
