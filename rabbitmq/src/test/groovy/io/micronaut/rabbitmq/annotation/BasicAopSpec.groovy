package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class BasicAopSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)
        consumer.messages.clear()

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

    void "test blocking publisher completion does not block consumers"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)
        consumer.messages.clear()
        CountDownLatch completionStarted = new CountDownLatch(1)
        CountDownLatch releaseCompletion = new CountDownLatch(1)
        AtomicReference<Throwable> publisherError = new AtomicReference<>()

        when:
        producer.goConfirm("def".bytes)
                .subscribe(new Subscriber<Void>() {

                    @Override
                    void onSubscribe(Subscription s) {
                        s.request(1)
                    }

                    @Override
                    void onNext(Void unused) {
                    }

                    @Override
                    void onError(Throwable t) {
                        publisherError.set(t)
                        completionStarted.countDown()
                    }

                    @Override
                    void onComplete() {
                        completionStarted.countDown()
                        try {
                            releaseCompletion.await(30, TimeUnit.SECONDS)
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt()
                            publisherError.set(e)
                        }
                    }
                })

        then:
        completionStarted.await(5, TimeUnit.SECONDS)

        when:
        producer.go("ghi".bytes)

        then:
        waitFor {
            assert consumer.messages.any { it == "ghi".bytes }
        }
        publisherError.get() == null

        cleanup:
        releaseCompletion.countDown()
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

        static List<byte[]> messages = new CopyOnWriteArrayList<>()

        @Queue("abc")
        void listen(byte[] data) {
            messages << data
        }
    }
}
