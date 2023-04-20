package io.micronaut.rabbitmq.docs.consumer.concurrent

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.framework.concurrency.eventually
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds

@MicronautTest
@Property(name = "spec.name", value = "ConcurrentSpec")
class ConcurrentSpec(productClient: ProductClient, productListener: ProductListener) : BehaviorSpec({

    val specName = javaClass.simpleName

    given("A basic producer and consumer") {
        `when`("The messages are published") {
            for (i in 0..3) {
                productClient.send("body".toByteArray())
            }

            then("The messages are received") {
                eventually(5.seconds) {
                    productListener.threads.size shouldBe 4
                }
            }
        }
    }
})
