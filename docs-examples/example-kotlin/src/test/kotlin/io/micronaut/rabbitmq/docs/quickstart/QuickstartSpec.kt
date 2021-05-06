package io.micronaut.rabbitmq.docs.quickstart

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.opentest4j.AssertionFailedError

class QuickstartSpec: AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = startContext(specName)

        `when`("the message is published") {
            val productListener = ctx.getBean(ProductListener::class.java)

// tag::producer[]
val productClient = ctx.getBean(ProductClient::class.java)
productClient.send("quickstart".toByteArray())
// end::producer[]

            then("the message is consumed") {
                eventually(10.seconds, AssertionFailedError::class.java) {
                    productListener.messageLengths.size shouldBe 1
                    productListener.messageLengths[0] shouldBe "quickstart"
                }
            }
        }

        Thread.sleep(1000)
        ctx.stop()
    }

})
