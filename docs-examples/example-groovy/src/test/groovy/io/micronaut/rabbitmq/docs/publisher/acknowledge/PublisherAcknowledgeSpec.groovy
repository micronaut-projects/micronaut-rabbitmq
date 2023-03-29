package io.micronaut.rabbitmq.docs.publisher.acknowledge

import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class PublisherAcknowledgeSpec extends AbstractRabbitMQTest {

    void "test publisher acknowledgement"() {
        given:
        startContext()
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger errorCount = new AtomicInteger(0)

        when:
        // tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        Publisher<Void> publisher = productClient.sendPublisher("publisher body".bytes)
        CompletableFuture<Void> future = productClient.sendFuture("future body".bytes)

        Subscriber<Void> subscriber = new Subscriber<Void>() {
            @Override
            void onSubscribe(Subscription subscription) {}

            @Override
            void onNext(Void aVoid) {
                throw new UnsupportedOperationException("Should never be called")
            }

            @Override
            void onError(Throwable throwable) {
                // if an error occurs
                errorCount.incrementAndGet()
            }

            @Override
            void onComplete() {
                // if the publish was acknowledged
                successCount.incrementAndGet()
            }
        }

        publisher.subscribe(subscriber)

        future.whenComplete {v, t ->
            if (t == null) {
                successCount.incrementAndGet()
            } else {
                errorCount.incrementAndGet()
            }
        }
// end::producer[]

        then:
        waitFor {
            assert errorCount.get() == 0
            assert successCount.get() == 2
        }
    }
}
