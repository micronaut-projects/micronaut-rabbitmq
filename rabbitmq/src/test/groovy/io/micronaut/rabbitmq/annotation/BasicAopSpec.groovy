package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class BasicAopSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go("abc".bytes)
        boolean success
        producer.goConfirm("def".bytes)
                .subscribe(new Subscriber<Void>() {

                    Subscription s

                    @Override
                    void onSubscribe(Subscription s) {
                        this.s = s
                        s.request(1)
                    }

                    @Override
                    void onNext(Void unused) {
                        s.request(1)
                    }

                    @Override
                    void onError(Throwable t) {}

                    @Override
                    void onComplete() {
                        success = true
                    }
                })

        then:
        waitFor {
            assert success
            assert consumer.messages.size() == 2
            assert consumer.messages[0] == "abc".bytes
            assert consumer.messages[1] == "def".bytes
        }
    }

    @Requires(property = "spec.name", value = "BasicAopSpec")
    @RabbitClient
    static interface MyProducer {

        @Binding("abc")
        void go(@MessageBody byte[] data)

        @Binding("abc")
        Publisher<Void> goConfirm(byte[] data)
    }

    @Requires(property = "spec.name", value = "BasicAopSpec")
    @RabbitListener
    static class MyConsumer {

        static List<byte[]> messages = []

        @Queue("abc")
        void listen(byte[] data) {
            messages << data
        }
    }
}
