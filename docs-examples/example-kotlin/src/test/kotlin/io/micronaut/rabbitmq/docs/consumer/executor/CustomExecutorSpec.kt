package io.micronaut.rabbitmq.docs.consumer.executor

import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import kotlin.time.Duration.Companion.seconds

class CustomExecutorSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    xgiven("A basic producer and consumer, disabled because it just waits indefinitely and never runs to completion") {
        val config = AbstractRabbitMQTest.getDefaultConfig(specName)
        config["micronaut.executors.product-listener.type"] = "FIXED"

        val ctx = startContext(config)

        `when`("the message is published") {
            val productListener = ctx.getBean(ProductListener::class.java)

// tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            productClient.send("custom-executor-test".toByteArray())
// end::producer[]

            then("the message is consumed") {
                eventually(10.seconds) {
                    productListener.messageLengths.size shouldBe 1
                    productListener.messageLengths[0] shouldBe "custom-executor-test"
                }
            }
        }

        Thread.sleep(200)
        ctx.stop()
    }
})
