package io.micronaut.configuration.rabbitmq.docs.publisher.acknowledge

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Maybe
import io.reactivex.MaybeObserver
import io.reactivex.disposables.Disposable
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Mono
import spock.util.concurrent.PollingConditions

import java.util.concurrent.atomic.AtomicInteger

class PublisherAcknowledgeSpec extends AbstractRabbitMQTest {

    void "test publisher acknowledgement"() {
        given:
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger errorCount = new AtomicInteger(0)

        when:
        // tag::producer[]
        ProductClient productClient = applicationContext.getBean(ProductClient)
        Completable completable = productClient.send("completable body".bytes)
        Maybe<Void> maybe = productClient.sendMaybe("maybe body".bytes)
        Mono<Void> mono = productClient.sendMono("mono body".bytes)
        Publisher<Void> publisher = productClient.sendPublisher("publisher body".bytes)

        completable.subscribe(new CompletableObserver() {
            @Override
            void onSubscribe(Disposable d) { }

            @Override
            void onComplete() {
                // if the publish was acknowledged
                successCount.incrementAndGet()
            }

            @Override
            void onError(Throwable e) {
                // if an error occurs
                errorCount.incrementAndGet()
            }
        })
        maybe.subscribe(new MaybeObserver<Void>() {
            @Override
            void onSubscribe(Disposable d) { }

            @Override
            void onSuccess(Void aVoid) {
                throw new UnsupportedOperationException("Should never be called");
            }

            @Override
            void onError(Throwable e) {
                // if an error occurs
                errorCount.incrementAndGet()
            }

            @Override
            void onComplete() {
                // if the publish was acknowledged
                successCount.incrementAndGet()
            }
        })
        Subscriber<Void> subscriber = new Subscriber<Void>() {
            @Override
            void onSubscribe(Subscription subscription) { }

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
        mono.subscribe(subscriber)
        publisher.subscribe(subscriber)
// end::producer[]

        then:
        conditions.eventually {
            errorCount.get() == 0
            successCount.get() == 4
        }

        cleanup:
        applicationContext.close()
    }

}
