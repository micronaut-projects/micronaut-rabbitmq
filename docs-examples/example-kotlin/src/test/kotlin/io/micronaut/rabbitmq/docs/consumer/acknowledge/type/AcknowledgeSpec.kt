package io.micronaut.rabbitmq.docs.consumer.acknowledge.type

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds

@MicronautTest
@Property(name = "spec.name", value = "AcknowledgeSpec")
class AcknowledgeSpec(productClient: ProductClient, productListener: ProductListener) : BehaviorSpec({

    val specName = javaClass.simpleName

    given("An acknowledgement argument") {
        `when`("The messages are published") {

            // tag::producer[]
            productClient.send("body".toByteArray())
            productClient.send("body".toByteArray())
            productClient.send("body".toByteArray())
            productClient.send("body".toByteArray())
            // end::producer[]

            then("The messages are received") {
                eventually(10.seconds) {
                    productListener.messageCount.get() shouldBe 5
                }
            }
        }
    }
})
