package io.micronaut.rabbitmq.docs.consumer.executor

import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CustomExecutorSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
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
                eventually(Duration.seconds(10)) {
                    productListener.messageLengths.size shouldBe 1
                    productListener.messageLengths[0] shouldBe "custom-executor-test"
                }
            }
        }

        Thread.sleep(200)
        ctx.stop()
    }
})
