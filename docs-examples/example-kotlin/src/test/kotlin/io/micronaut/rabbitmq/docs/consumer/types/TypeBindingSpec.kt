package io.micronaut.rabbitmq.docs.consumer.types

import io.kotest.assertions.timing.eventually
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TypeBindingSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = startContext(specName)

        `when`("The messages are published") {
            val productListener = ctx.getBean(ProductListener::class.java)

            // tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            productClient.send("body".toByteArray(), "text/html")
            productClient.send("body2".toByteArray(), "application/json")
            productClient.send("body3".toByteArray(), "text/xml")
            // end::producer[]

            then("The messages are received") {
                eventually(Duration.seconds(10)) {
                    productListener.messages.size shouldBe 3
                    productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [text/html]"
                    productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [application/json]"
                    productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [text/xml]"
                }
            }
        }

        ctx.stop()
    }
})
