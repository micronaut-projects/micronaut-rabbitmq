package io.micronaut.rabbitmq.docs.consumer.custom.type

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import kotlin.time.Duration.Companion.seconds
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "ProductInfoSpec")
class ProductInfoSpec : TestPropertyProvider, AnnotationSpec() {

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }

    @Inject
    lateinit var productClient: ProductClient

    @Inject
    lateinit var productListener: ProductListener

    @Test
    suspend fun testCustomTypeBinder() {

        // tag::producer[]
        productClient.send("body".toByteArray())
        productClient.send("medium", 20L, "body2".toByteArray())
        productClient.send(null, 30L, "body3".toByteArray())
        // end::producer[]

        eventually(10.seconds) {
            productListener.messages.size shouldBe 3
            productListener.messages shouldExist { p -> p.size == "small" && p.count == 10L && p.sealed }
            productListener.messages shouldExist { p -> p.size == "medium" && p.count == 20L && p.sealed }
            productListener.messages shouldExist { p -> p.size == null && p.count == 30L && p.sealed }
        }
    }
}
