package io.micronaut.rabbitmq.docs.consumer.acknowledge.type

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.opentest4j.AssertionFailedError

class AcknowledgeSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("An acknowledgement argument") {
        val ctx = startContext(specName)

        `when`("The messages are published") {
            val productListener = ctx.getBean(ProductListener::class.java)

            // tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            productClient.send("body".toByteArray())
            productClient.send("body".toByteArray())
            productClient.send("body".toByteArray())
            productClient.send("body".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds, AssertionFailedError::class.java) {
                    productListener.messageCount.get() shouldBe 5
                }
            }
        }

        ctx.stop()
    }
})