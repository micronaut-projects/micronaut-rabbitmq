package io.micronaut.rabbitmq.docs.consumer.custom.annotation

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@MicronautTest
@Property(name = "spec.name", value = "DeliveryTagSpec")
class DeliveryTagSpec(productClient: ProductClient, productListener: ProductListener) : BehaviorSpec({

    val specName = javaClass.simpleName

    given("Using a custom annotation binder") {

        `when`("The messages are published") {
            // tag::producer[]
            productClient.send("body".toByteArray())
            productClient.send("body2".toByteArray())
            productClient.send("body3".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds) {
                    productListener.messages.size shouldBe 3
                }
            }
        }
    }
})
