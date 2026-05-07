package io.micronaut.rabbitmq.docs.headers

import io.kotest.assertions.nondeterministic.eventually
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
@Property(name = "spec.name", value = "HeadersSpec")
class HeadersSpec : TestPropertyProvider, AnnotationSpec() {

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
        productClient.send("body".toByteArray())
        productClient.send("medium", 20L, "body2".toByteArray())
        productClient.send(null, 30L, "body3".toByteArray())
        productClient.send(mapOf<String, Any>("productSize" to "large", "x-product-count" to 40L), "body4".toByteArray())
        // end::producer[]

        eventually(10.seconds) {
            productListener.messageProperties.size shouldBe 4
            productListener.messageProperties shouldContain "true|10|small"
            productListener.messageProperties shouldContain "true|20|medium"
            productListener.messageProperties shouldContain "true|30|null"
            productListener.messageProperties shouldContain "true|40|large"
        }
    }
}
