package io.micronaut.rabbitmq.docs.rpc

import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import reactor.core.publisher.Mono
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
                Mono.from(productClient.sendReactive("world")).block() shouldBe "WORLD"
            }
        }

        ctx.stop()
    }

})
