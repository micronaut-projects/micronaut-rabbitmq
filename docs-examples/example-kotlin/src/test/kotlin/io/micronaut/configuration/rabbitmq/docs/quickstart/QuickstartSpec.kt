package io.micronaut.configuration.rabbitmq.docs.quickstart

import io.kotlintest.*
import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import org.opentest4j.AssertionFailedError

class QuickstartSpec: AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = startContext(specName)

        `when`("the message is published") {
            val productListener = ctx.getBean(ProductListener::class.java)

// tag::producer[]
val productClient = ctx.getBean(ProductClient::class.java)
productClient.send("message body".toByteArray())
// end::producer[]

            then("the message is consumed") {
                eventually(10.seconds, AssertionFailedError::class.java) {
                    productListener.messageLengths.size shouldBe 1
                    productListener.messageLengths[0] shouldBe 12
                }
            }
        }

        ctx.stop()
    }

})
