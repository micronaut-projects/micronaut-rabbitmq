package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@MicronautTest
@Property(name = "spec.name", value = "ProductInfoSpec")
class ProductInfoSpec(productClient: ProductClient, productListener: ProductListener) : BehaviorSpec({

    val specName = javaClass.simpleName

    given("A custom type binder") {
        `when`("The messages are published") {

            // tag::producer[]
            productClient.send("body".toByteArray())
            productClient.send("medium", 20L, "body2".toByteArray())
            productClient.send(null, 30L, "body3".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds) {
                    productListener.messages.size shouldBe 3
                    productListener.messages shouldExist { p -> p.size == "small" && p.count == 10L && p.sealed }
                    productListener.messages shouldExist { p -> p.size == "medium" && p.count == 20L && p.sealed }
                    productListener.messages shouldExist { p -> p.size == null && p.count == 30L && p.sealed }
                }
            }
        }
    }
})
