package io.micronaut.configuration.rabbitmq.docs.headers

import io.kotlintest.eventually
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import org.opentest4j.AssertionFailedError

class HeadersSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        val ctx = startContext(specName)

        `when`("The messages are published") {
            val productListener = ctx.getBean(ProductListener::class.java)

            // tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            productClient.send("body".toByteArray())
            productClient.send("medium", 20L, "body2".toByteArray())
            productClient.send(null, 30L, "body3".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds, AssertionFailedError::class.java) {
                    productListener.messageProperties.size shouldBe 3
                    productListener.messageProperties shouldContain "true|10|small"
                    productListener.messageProperties shouldContain "true|20|medium"
                    productListener.messageProperties shouldContain "true|30|null"
                }
            }
        }

        ctx.stop()
    }
})