package io.micronaut.rabbitmq.docs.consumer.acknowledge.type

import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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
                    eventually(Duration.seconds(10)) {
                    productListener.messageCount.get() shouldBe 5
                }
            }
        }

        ctx.stop()
    }
})