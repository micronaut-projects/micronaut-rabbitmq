package io.micronaut.rabbitmq.docs.consumer.executor

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.opentest4j.AssertionFailedError

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
                eventually(10.seconds, AssertionFailedError::class.java) {
                    productListener.messageLengths.size shouldBe 1
                    productListener.messageLengths[0] shouldBe "custom-executor-test"
                }
            }
        }

        Thread.sleep(200)
        ctx.stop()
    }
})
