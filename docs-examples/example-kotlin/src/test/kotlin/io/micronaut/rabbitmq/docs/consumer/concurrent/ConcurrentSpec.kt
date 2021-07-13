package io.micronaut.rabbitmq.docs.consumer.concurrent

import io.kotest.framework.concurrency.eventually
import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ConcurrentSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = startContext(specName)

        `when`("The messages are published") {
            val productListener = ctx.getBean(ProductListener::class.java)
            val productClient = ctx.getBean(ProductClient::class.java)
            for (i in 0..3) {
                productClient.send("body".toByteArray())
            }

            then("The messages are received") {
                eventually(Duration.seconds(5)) {
                    productListener.threads.size shouldBe 4
                }
            }
        }

        ctx.close()
    }
})
