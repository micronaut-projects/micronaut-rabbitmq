package io.micronaut.rabbitmq.docs.publisher.acknowledge;

import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PublisherAcknowledgeSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("Publisher acknowledgement") {
        val ctx = startContext(specName)
        val successCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)

        `when`("The messages are published") {
            // tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            val publisher = productClient.sendPublisher("publisher body".toByteArray())

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
// end::producer[]

            then("The messages are published") {
                eventually(Duration.seconds(10)) {
                    errorCount.get() shouldBe 0
                    successCount.get() shouldBe 1
                }
            }
        }

        ctx.stop()
    }
})
