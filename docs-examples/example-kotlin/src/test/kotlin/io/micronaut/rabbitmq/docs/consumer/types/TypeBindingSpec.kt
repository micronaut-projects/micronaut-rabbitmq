package io.micronaut.rabbitmq.docs.consumer.types

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import kotlin.time.Duration.Companion.seconds
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "TypeBindingSpec")
class TypeBindingSpec : TestPropertyProvider, AnnotationSpec() {

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
        productClient.send("body".toByteArray(), "text/html")
        productClient.send("body2".toByteArray(), "application/json")
        productClient.send("body3".toByteArray(), "text/xml")
        // end::producer[]

        eventually(10.seconds) {
            productListener.messages.size shouldBe 3
            productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [text/html]"
            productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [application/json]"
            productListener.messages shouldContain "exchange: [], routingKey: [product], contentType: [text/xml]"
        }
    }
}
