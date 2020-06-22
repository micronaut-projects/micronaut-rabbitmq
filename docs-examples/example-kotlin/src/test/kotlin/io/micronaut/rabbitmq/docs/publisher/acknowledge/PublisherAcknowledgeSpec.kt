package io.micronaut.rabbitmq.docs.publisher.acknowledge;

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.reactivex.CompletableObserver
import io.reactivex.MaybeObserver
import io.reactivex.disposables.Disposable
import org.opentest4j.AssertionFailedError
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicInteger

class PublisherAcknowledgeSpec : AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("Publisher acknowledgement") {
        val ctx = startContext(specName)
        val successCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)

        `when`("The messages are published") {
            // tag::producer[]
            val productClient = ctx.getBean(ProductClient::class.java)
            val completable = productClient.send("completable body".toByteArray())
            val maybe = productClient.sendMaybe("maybe body".toByteArray())
            val mono = productClient.sendMono("mono body".toByteArray())
            val publisher = productClient.sendPublisher("publisher body".toByteArray())

            completable.subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) { }

                override fun onComplete() {
                    // if the publish was acknowledged
                    successCount.incrementAndGet()
                }

                override fun onError(e: Throwable) {
                    // if an error occurs
                    errorCount.incrementAndGet()
                }
            })
            maybe.subscribe(object : MaybeObserver<Void> {
                override fun onSubscribe(d: Disposable) { }

                override fun onSuccess(aVoid: Void) {
                    throw UnsupportedOperationException("Should never be called")
                }

                override fun onError(e: Throwable) {
                    // if an error occurs
                    errorCount.incrementAndGet();
                }

                override fun onComplete() {
                    // if the publish was acknowledged
                    successCount.incrementAndGet()
                }
            })
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
            mono.subscribe(subscriber)
            publisher.subscribe(subscriber)
// end::producer[]

            then("The messages are published") {
                eventually(10.seconds, AssertionFailedError::class.java) {
                    errorCount.get() shouldBe 0
                    successCount.get() shouldBe 4
                }
            }
        }

        ctx.stop()
    }
})