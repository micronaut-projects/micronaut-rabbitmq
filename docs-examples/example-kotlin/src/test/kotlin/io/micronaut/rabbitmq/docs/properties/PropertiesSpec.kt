package io.micronaut.rabbitmq.docs.properties

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
@Property(name = "spec.name", value = "PropertiesSpec")
class PropertiesSpec :TestPropertyProvider, AnnotationSpec() {

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }

    @Inject
    lateinit var productClient: ProductClient

    @Inject
    lateinit var productListener: ProductListener

    @Test
    suspend fun testPublishingAndReceivingProperties() {
        // tag::producer[]
        productClient.send("body".toByteArray())
        productClient.send("guest", "text/html", "body2".toByteArray())
        productClient.send("guest", null, "body3".toByteArray())
        // end::producer[]

        eventually(10.seconds) {
            productListener.messageProperties.size shouldBe 3
            productListener.messageProperties shouldContain "guest|application/json|myApp"
            productListener.messageProperties shouldContain "guest|text/html|myApp"
            productListener.messageProperties shouldContain "guest|null|myApp"
        }
    }
}
