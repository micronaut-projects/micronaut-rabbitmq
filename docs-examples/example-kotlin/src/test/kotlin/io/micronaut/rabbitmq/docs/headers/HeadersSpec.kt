package io.micronaut.rabbitmq.docs.headers

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@MicronautTest
@Property(name = "spec.name", value = "HeadersSpec")
class HeadersSpec(productClient: ProductClient, productListener: ProductListener) : BehaviorSpec({

    given("A basic producer and consumer") {
        `when`("The messages are published") {

            // tag::producer[]
            productClient.send("body".toByteArray())
            productClient.send("medium", 20L, "body2".toByteArray())
            productClient.send(null, 30L, "body3".toByteArray())
            productClient.send(mapOf<String, Any>("productSize" to "large", "x-product-count" to 40L), "body4".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds) {
                    productListener.messageProperties.size shouldBe 4
                    productListener.messageProperties shouldContain "true|10|small"
                    productListener.messageProperties shouldContain "true|20|medium"
                    productListener.messageProperties shouldContain "true|30|null"
                    productListener.messageProperties shouldContain "true|40|large"
                }
            }
        }
    }
})
