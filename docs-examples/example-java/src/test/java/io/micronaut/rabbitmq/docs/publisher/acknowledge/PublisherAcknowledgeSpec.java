package io.micronaut.rabbitmq.docs.publisher.acknowledge;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class PublisherAcknowledgeSpec extends AbstractRabbitMQTest {

    @Test
    void testPublisherAcknowledgement() {
        ApplicationContext applicationContext = startContext();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

// tag::producer[]
ProductClient productClient = applicationContext.getBean(ProductClient.class);
Publisher<Void> publisher = productClient.sendPublisher("publisher body".getBytes());
CompletableFuture<Void> future = productClient.sendFuture("future body".getBytes());

Subscriber<Void> subscriber = new Subscriber<Void>() {
    @Override
    public void onSubscribe(Subscription subscription) { }

    @Override
    public void onNext(Void aVoid) {
        throw new UnsupportedOperationException("Should never be called");
    }

    @Override
    public void onError(Throwable throwable) {
        // if an error occurs
        errorCount.incrementAndGet();
    }

    @Override
    public void onComplete() {
        // if the publish was acknowledged
        successCount.incrementAndGet();
    }
};
publisher.subscribe(subscriber);
future.whenComplete((v, t) -> {
    if (t == null) {
        successCount.incrementAndGet();
    } else {
        errorCount.incrementAndGet();
    }
});
// end::producer[]

        try {
            await().atMost(5, SECONDS).until(() ->
                    errorCount.get() == 0 &&
                    successCount.get() == 2
            );
        } finally {
            applicationContext.close();
        }
    }

}
