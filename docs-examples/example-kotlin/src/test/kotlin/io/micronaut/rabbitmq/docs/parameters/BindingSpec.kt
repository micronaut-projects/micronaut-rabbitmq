package io.micronaut.rabbitmq.docs.parameters

import io.kotest.assertions.timing.eventually
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class BindingSpec: AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = startContext(specName)

        `when`("The messages are published") {
            val productListener = ctx.getBean(ProductListener::class.java)

            // tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            productClient.send("message body".toByteArray())
            productClient.send("product", "message body2".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds) {
                    productListener.messageLengths.size shouldBe 2
                    productListener.messageLengths shouldContain 12
                    productListener.messageLengths shouldContain 13
                }
            }
        }

        ctx.stop()
    }
})
