package io.micronaut.configuration.rabbitmq.docs.parameters

import io.kotlintest.*
import io.kotlintest.matchers.collections.shouldContain
import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import org.opentest4j.AssertionFailedError

class BindingSpec: AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = ApplicationContext.run(
                mapOf("rabbitmq.port" to rabbitContainer.getMappedPort(5672),
                        "spec.name" to specName))

        `when`("The messages are published") {
            val productListener = ctx.getBean(ProductListener::class.java)

            // tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            productClient.send("message body".toByteArray())
            productClient.send("product", "message body2".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds, AssertionFailedError::class.java) {
                    productListener.messageLengths.size shouldBe 2
                    productListener.messageLengths shouldContain 12
                    productListener.messageLengths shouldContain 13
                }
            }
        }

        ctx.stop()
    }
})
