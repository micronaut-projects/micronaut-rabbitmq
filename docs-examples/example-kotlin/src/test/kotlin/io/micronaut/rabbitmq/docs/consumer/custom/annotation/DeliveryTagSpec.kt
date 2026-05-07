package io.micronaut.rabbitmq.docs.consumer.custom.annotation

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
@Property(name = "spec.name", value = "DeliveryTagSpec")
class DeliveryTagSpec : TestPropertyProvider, AnnotationSpec() {

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }

    @Inject
    lateinit var productClient: ProductClient

    @Inject
    lateinit var productListener: ProductListener

    @Test
    suspend fun testUsingCustomAnnotationBinder() {
        // tag::producer[]
        productClient.send("body".toByteArray())
        productClient.send("body2".toByteArray())
        productClient.send("body3".toByteArray())
        // end::producer[]

        eventually(10.seconds) {
            productListener.messages.size shouldBe 3
        }
    }
}
