package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class ColdObservableSpec extends AbstractRabbitMQTest {

    void "test publisher acknowledgement returns a cold observable"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        Publisher<Void> publisher = producer.publisher("def".bytes)

        then:
        waitFor {
            consumer.messages.size() == 0
        }

        when:
        publisher.subscribe(new Subscriber<Void>() {

            @Override
            void onSubscribe(Subscription s) {
                s.request(1)
            }

            @Override void onNext(Void unused) {}
            @Override void onError(Throwable t) {}
            @Override void onComplete() {}
        })

        then:
        waitFor {
            consumer.messages.size() == 1
        }
    }

    @Requires(property = "spec.name", value = "ColdObservableSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("abc")
        Publisher<Void> publisher(byte[] data)
    }

    @Requires(property = "spec.name", value = "ColdObservableSpec")
    @RabbitListener
    static class MyConsumer {

        static List<byte[]> messages = []

        @Queue("abc")
        void listen(byte[] data) {
            messages << data
        }
    }
}
