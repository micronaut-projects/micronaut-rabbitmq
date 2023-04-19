package io.micronaut.rabbitmq.docs.consumer.executor

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlin.time.Duration.Companion.seconds

@MicronautTest(rebuildContext = true)
@Property(name = "micronaut.executors.product-listener.type", value = "FIXED")
@Property(name = "spec.name", value = "CustomExecutorSpec")
class CustomExecutorSpec : AnnotationSpec() {
    @Inject
    lateinit var productClient: ProductClient

    @Inject
    lateinit var productListener: ProductListener

    @Test
    suspend fun testBasicConsumerAndProducer() {


// tag::producer[]
        productClient.send("custom-executor-test".toByteArray())
// end::producer[]

        eventually(10.seconds) {
            productListener.messageLengths.size shouldBe 1
            productListener.messageLengths[0] shouldBe "custom-executor-test"
        }
    }
}
