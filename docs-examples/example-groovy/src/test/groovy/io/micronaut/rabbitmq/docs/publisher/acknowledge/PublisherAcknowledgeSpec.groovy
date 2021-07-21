package io.micronaut.rabbitmq.docs.publisher.acknowledge

import io.micronaut.context.ApplicationContext
import io.micronaut.rabbitmq.AbstractRabbitMQTest
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
        Publisher<Void> publisher = productClient.sendPublisher("publisher body".bytes)

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
        publisher.subscribe(subscriber)
// end::producer[]

        then:
        conditions.eventually {
            errorCount.get() == 0
            successCount.get() == 1
        }

        cleanup:
        applicationContext.close()
    }

}
