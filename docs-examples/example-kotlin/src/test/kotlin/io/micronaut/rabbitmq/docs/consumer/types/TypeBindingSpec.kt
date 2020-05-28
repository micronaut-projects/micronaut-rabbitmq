package io.micronaut.rabbitmq.docs.consumer.types

import io.kotlintest.eventually
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.opentest4j.AssertionFailedError

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
                eventually(10.seconds, AssertionFailedError::class.java) {
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