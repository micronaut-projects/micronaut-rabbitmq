package io.micronaut.rabbitmq.docs.publisher.acknowledge

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await

@MicronautTest
@Property(name = "spec.name", value = "PublisherAcknowledgeSpec")
class PublisherAcknowledgeSpec extends Specification {
    @Inject ProductClient productClient
    void "test publisher acknowledgement"() {
        given:
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger errorCount = new AtomicInteger(0)

        when:
        // tag::producer[]
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
        await().atMost(10, SECONDS).until {
            successCount.get() == 2
        }

        then:
        errorCount.get() == 0
        successCount.get() == 2
    }
}
