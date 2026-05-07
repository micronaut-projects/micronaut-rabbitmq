package io.micronaut.rabbitmq.docs.publisher.acknowledge;

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "PublisherAcknowledgeSpec")
class PublisherAcknowledgeSpec : TestPropertyProvider, AnnotationSpec() {

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }

    @Inject
    lateinit var productClient: ProductClient

    @Test
    suspend fun testPublisherAcknowledgement() = coroutineScope {
        val successCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)

        // tag::producer[]
        val publisher = productClient.sendPublisher("publisher body".toByteArray())
        val future = productClient.sendFuture("future body".toByteArray())
        val deferred = async {
            productClient.sendSuspend("suspend body".toByteArray())
        }

        val subscriber = (object: Subscriber<Void> {
            override fun onSubscribe(subscription: Subscription) { }

            override fun onNext(aVoid: Void) {
                throw UnsupportedOperationException("Should never be called")
            }

            override fun onError(throwable: Throwable) {
                // if an error occurs
                errorCount.incrementAndGet()
            }

            override fun onComplete() {
                // if the publish was acknowledged
                successCount.incrementAndGet()
            }
        })
        publisher.subscribe(subscriber)
        future.handle { _, t ->
            if (t == null) {
                successCount.incrementAndGet()
            } else {
                errorCount.incrementAndGet()
            }
        }
        deferred.invokeOnCompletion {
            if (it == null) {
                successCount.incrementAndGet()
            } else {
                errorCount.incrementAndGet()
            }
        }
// end::producer[]

        eventually(10.seconds) {
            errorCount.get() shouldBe 0
            successCount.get() shouldBe 3
        }
    }

}
