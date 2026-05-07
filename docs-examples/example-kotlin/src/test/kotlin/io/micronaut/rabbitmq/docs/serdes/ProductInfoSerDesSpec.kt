package io.micronaut.rabbitmq.docs.serdes

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
@Property(name = "spec.name", value = "ProductInfoSerDesSpec")
class ProductInfoSerDesSpec : TestPropertyProvider, AnnotationSpec() {

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }

    @Inject
    lateinit var productClient: ProductClient

    @Inject
    lateinit var listener: ProductListener

    @Test
    suspend fun testBasicProducerAndConsumer() {

// tag::producer[]
        productClient.send(ProductInfo("small", 10L, true))
        productClient.send(ProductInfo("medium", 20L, true))
        productClient.send(ProductInfo(null, 30L, false))
// end::producer[]

        eventually(10.seconds) {
            listener.messages.size shouldBe 3
            listener.messages shouldExist { p -> p.size == "small" && p.count == 10L && p.sealed }
            listener.messages shouldExist { p -> p.size == "medium" && p.count == 20L && p.sealed }
            listener.messages shouldExist { p -> p.size == null && p.count == 30L && !p.sealed }
        }
    }
}
