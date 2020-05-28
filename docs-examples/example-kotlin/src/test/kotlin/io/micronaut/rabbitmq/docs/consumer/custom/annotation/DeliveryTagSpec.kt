package io.micronaut.rabbitmq.docs.consumer.custom.annotation

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.opentest4j.AssertionFailedError

class DeliveryTagSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("Using a custom annotation binder") {
        val ctx = startContext(specName)

        `when`("The messages are published") {
            val productListener = ctx.getBean(ProductListener::class.java)

            // tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            productClient.send("body".toByteArray())
            productClient.send("body2".toByteArray())
            productClient.send("body3".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds, AssertionFailedError::class.java) {
                    productListener.messages.size shouldBe 3
                }
            }
        }

        ctx.stop()
    }
})