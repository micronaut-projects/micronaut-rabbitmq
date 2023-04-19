package io.micronaut.rabbitmq.docs.consumer.types

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@MicronautTest
@Property(name = "spec.name", value = "TypeBindingSpec")
class TypeBindingSpec(productClient: ProductClient, productListener: ProductListener) : BehaviorSpec({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {

        `when`("The messages are published") {

            // tag::producer[]
            productClient.send("body".toByteArray(), "text/html")
            productClient.send("body2".toByteArray(), "application/json")
            productClient.send("body3".toByteArray(), "text/xml")
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds) {
                    productListener.messages.size shouldBe 3
                    productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [text/html]"
                    productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [application/json]"
                    productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [text/xml]"
                }
            }
        }
    }
})
