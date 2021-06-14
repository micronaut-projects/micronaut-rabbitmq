package io.micronaut.rabbitmq.docs.headers

import io.kotest.assertions.timing.eventually
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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
            productClient.send(mapOf<String, Any>("productSize" to "large", "x-product-count" to 40L), "body4".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(Duration.seconds(10)) {
                    productListener.messageProperties.size shouldBe 4
                    productListener.messageProperties shouldContain "true|10|small"
                    productListener.messageProperties shouldContain "true|20|medium"
                    productListener.messageProperties shouldContain "true|30|null"
                    productListener.messageProperties shouldContain "true|40|large"
                }
            }
        }

        ctx.stop()
    }
})
