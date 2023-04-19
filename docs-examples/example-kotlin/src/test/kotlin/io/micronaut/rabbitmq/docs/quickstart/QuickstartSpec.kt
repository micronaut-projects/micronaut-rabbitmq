package io.micronaut.rabbitmq.docs.quickstart

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@MicronautTest
@Property(name = "spec.name", value = "QuickstartSpec")
class QuickstartSpec(productClient: ProductClient, productListener: ProductListener): BehaviorSpec({

    given("A basic producer and consumer") {
        `when`("the message is published") {

// tag::producer[]
productClient.send("quickstart".toByteArray())
// end::producer[]

            then("the message is consumed") {
                eventually(10.seconds) {
                    productListener.messageLengths.size shouldBe 1
                    productListener.messageLengths[0] shouldBe "quickstart"
                }
            }
        }
    }
})
