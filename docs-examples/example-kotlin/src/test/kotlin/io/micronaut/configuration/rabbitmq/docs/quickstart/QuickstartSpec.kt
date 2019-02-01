package io.micronaut.configuration.rabbitmq.docs.quickstart

import io.kotlintest.*
import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext

class QuickstartSpec: AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = ApplicationContext.run(
                mapOf("rabbitmq.port" to rabbitContainer.getMappedPort(5672),
                        "spec.name" to specName))

        `when`("the message is published") {
            val productListener = ctx.getBean(ProductListener::class.java)

// tag::producer[]
val productClient = ctx.getBean(ProductClient::class.java)
productClient.send("message body".toByteArray())
// end::producer[]

            then("the message is consumed") {
                eventually(10.seconds) {
                    productListener.messageLengths.size shouldBe 1
                    productListener.messageLengths[0] shouldBe 12
                }
            }
        }

        ctx.stop()
    }

})
