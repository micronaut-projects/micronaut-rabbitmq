package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.kotlintest.eventually
import io.kotlintest.matchers.collections.shouldExist
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.opentest4j.AssertionFailedError

class ProductInfoSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("A custom type binder") {
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
                    productListener.messages.size shouldBe 3
                    productListener.messages shouldExist { p -> p.size == "small" && p.count == 10L && p.sealed }
                    productListener.messages shouldExist { p -> p.size == "medium" && p.count == 20L && p.sealed }
                    productListener.messages shouldExist { p -> p.size == null && p.count == 30L && p.sealed }
                }
            }
        }

        ctx.stop()
    }
})