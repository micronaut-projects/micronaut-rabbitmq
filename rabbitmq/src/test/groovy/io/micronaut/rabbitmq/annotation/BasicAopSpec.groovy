package io.micronaut.rabbitmq.annotation

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.util.concurrent.PollingConditions

class BasicAopSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
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
                    void onError(Throwable t) {

                    }

                    @Override
                    void onComplete() {
                        success = true
                    }
                })

        then:
        success
        conditions.eventually {
            consumer.messages.size() == 2
            consumer.messages[0] == "abc".bytes
            consumer.messages[1] == "def".bytes
        }

        cleanup:
        applicationContext.close()
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

        public static List<byte[]> messages = []

        @Queue("abc")
        void listen(byte[] data) {
            messages.add(data)
        }
    }
}
