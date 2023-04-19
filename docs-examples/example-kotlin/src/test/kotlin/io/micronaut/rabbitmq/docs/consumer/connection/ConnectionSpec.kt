package io.micronaut.rabbitmq.docs.consumer.connection

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import io.micronaut.testresources.client.TestResourcesClientFactory
import jakarta.inject.Inject
import java.net.URI
import java.util.Map
import kotlin.time.Duration.Companion.seconds

@MicronautTest
@Property(name = "spec.name", value = "ConnectionSpec")
class ConnectionSpec
    : TestPropertyProvider, AnnotationSpec() {
    @Inject
    lateinit var productClient: ProductClient
    @Inject
    lateinit var productListener: ProductListener

    @Test
    suspend fun testBasicProducerAndConsumer() {
// tag::producer[]
        productClient.send("connection-test".toByteArray())
// end::producer[]
        eventually(10.seconds) {
            productListener.messageLengths.size shouldBe 1
            productListener.messageLengths[0] shouldBe "connection-test"
        }
    }

    override fun getProperties(): MutableMap<String, String> {
        val client = TestResourcesClientFactory.fromSystemProperties().get()
        val rabbitURI = client.resolve("rabbitmq.uri", Map.of(), Map.of())
        return rabbitURI
            .map { uri: String ->
                Map.of(
                    "rabbitmq.servers.product-cluster.port",
                    URI.create(uri).port.toString()
                )
            }
            .orElse(emptyMap())
    }
}
