package io.micronaut.rabbitmq.annotation

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import spock.util.concurrent.PollingConditions

class ColdObservableSpec extends AbstractRabbitMQTest {

    void "test publisher acknowledgement returns a cold observable"() {
            ApplicationContext applicationContext = ApplicationContext.run(
                    ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                     "spec.name": getClass().simpleName], "test")
            PollingConditions conditions = new PollingConditions(timeout: 10, initialDelay: 1)
            MyProducer producer = applicationContext.getBean(MyProducer)
            MyConsumer consumer = applicationContext.getBean(MyConsumer)

            when:
            Completable completable = producer.completable("def".bytes)
            Single<Void> single = producer.single("ghi".bytes)
            Flowable<Void> flowable = producer.flowable("jkl".bytes)
            Maybe<Void> maybe = producer.maybe("mno".bytes)

            then:
            conditions.eventually {
                consumer.messages.size() == 0
            }

            when:
            completable.subscribe()

            then:
            conditions.eventually {
                consumer.messages.size() == 1
            }

            when:
            single.subscribe()

            then:
            conditions.eventually {
                consumer.messages.size() == 2
            }

            when:
            flowable.subscribe()

            then:
            conditions.eventually {
                consumer.messages.size() == 3
            }

            when:
            maybe.subscribe()

            then:
            conditions.eventually {
                consumer.messages.size() == 4
            }

            cleanup:
            applicationContext.close()



    }

    @Requires(property = "spec.name", value = "ColdObservableSpec")
    @RabbitClient
    static interface MyProducer {

        @Binding("abc")
        Completable completable(byte[] data)

        @Binding("abc")
        Single<Void> single(byte[] data)

        @Binding("abc")
        Flowable<Void> flowable(byte[] data)

        @Binding("abc")
        Maybe<Void> maybe(byte[] data)

    }

    @Requires(property = "spec.name", value = "ColdObservableSpec")
    @RabbitListener
    static class MyConsumer {

        public static List<byte[]> messages = []

        @Queue("abc")
        void listen(byte[] data) {
            messages.add(data)
        }
    }
}
