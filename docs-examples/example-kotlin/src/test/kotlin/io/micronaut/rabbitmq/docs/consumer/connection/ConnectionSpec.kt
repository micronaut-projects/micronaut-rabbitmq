package io.micronaut.rabbitmq.docs.consumer.connection

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.rabbitmq.testcontainers.RabbitMQ
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import java.net.URI
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

    override fun getProperties(): Map<String, String> {
        val m: MutableMap<String, String> = HashMap(RabbitMQ.getProperties())
        val uri: String = m["rabbitmq.uri"]!!
        val host = URI.create(uri).host
        val port = URI.create(uri).port.toString()
        m["rabbitmq.servers.product-cluster.uri"] = uri
        m["rabbitmq.servers.product-cluster.host"] = host
        m["rabbitmq.servers.product-cluster.port"] = port
        return m
    }
}
