package io.micronaut.rabbitmq.docs.properties

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@MicronautTest
@Property(name = "spec.name", value = "PropertiesSpec")
class PropertiesSpec(productClient: ProductClient, productListener: ProductListener) : BehaviorSpec({

    given("publishing and receiving properties") {
        `when`("messages with properties are sent") {
            // tag::producer[]
            productClient.send("body".toByteArray())
            productClient.send("guest", "text/html", "body2".toByteArray())
            productClient.send("guest", null, "body3".toByteArray())
            // end::producer[]

            then("the messages are received") {

                eventually(10.seconds) {
                    productListener.messageProperties.size shouldBe 3
                    productListener.messageProperties shouldContain "guest|application/json|myApp"
                    productListener.messageProperties shouldContain "guest|text/html|myApp"
                    productListener.messageProperties shouldContain "guest|null|myApp"
                }
            }
        }
    }
})
