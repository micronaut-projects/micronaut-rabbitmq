package io.micronaut.rabbitmq.docs.rpc

import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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
