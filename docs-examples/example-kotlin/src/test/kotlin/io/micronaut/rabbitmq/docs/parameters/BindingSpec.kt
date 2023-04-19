package io.micronaut.rabbitmq.docs.parameters

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@MicronautTest
@Property(name = "spec.name", value = "BindingSpec")
class BindingSpec(productClient: ProductClient, productListener: ProductListener): BehaviorSpec({

    given("A basic producer and consumer") {
        `when`("The messages are published") {

            // tag::producer[]
            productClient.send("message body".toByteArray())
            productClient.send("product", "message body2".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds) {
                    productListener.messageLengths.size shouldBe 2
                    productListener.messageLengths shouldContain 12
                    productListener.messageLengths shouldContain 13
                }
            }
        }
    }
})
