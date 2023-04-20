package io.micronaut.rabbitmq.docs.publisher.acknowledge;

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlinx.coroutines.async
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

@MicronautTest
@Property(name = "spec.name", value = "PublisherAcknowledgeSpec")
class PublisherAcknowledgeSpec(productClient: ProductClient) : BehaviorSpec({

    given("Publisher acknowledgement") {
        val successCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)

        `when`("The messages are published") {
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

            then("The messages are published") {
                eventually(10.seconds) {
                    errorCount.get() shouldBe 0
                    successCount.get() shouldBe 3
                }
            }
        }
    }
})
