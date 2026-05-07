package io.micronaut.rabbitmq.docs.consumer.concurrent

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
@Property(name = "spec.name", value = "ConcurrentSpec")
class ConcurrentSpec : TestPropertyProvider, AnnotationSpec() {

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }

    @Inject
    lateinit var productClient: ProductClient

    @Inject
    lateinit var productListener: ProductListener

    @Test
    suspend fun testBasicProducerAndConsumer() {
        for (i in 0..3) {
            productClient.send("body".toByteArray())
        }

        eventually(5.seconds) {
            productListener.threads.size shouldBe 4
        }
    }
}
