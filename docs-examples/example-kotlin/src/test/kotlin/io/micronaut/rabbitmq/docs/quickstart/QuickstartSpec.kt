package io.micronaut.rabbitmq.docs.quickstart

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import kotlin.time.Duration.Companion.seconds
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "QuickstartSpec")
class QuickstartSpec : TestPropertyProvider, AnnotationSpec() {

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }

    @Inject
    lateinit var productClient: ProductClient

    @Inject
    lateinit var productListener: ProductListener

    @Test
    suspend fun testBasicProducerAndConsumer() {

// tag::producer[]
productClient.send("quickstart".toByteArray())
// end::producer[]

        eventually(10.seconds) {
            productListener.messageLengths.size shouldBe 1
            productListener.messageLengths[0] shouldBe "quickstart"
        }
    }
}
