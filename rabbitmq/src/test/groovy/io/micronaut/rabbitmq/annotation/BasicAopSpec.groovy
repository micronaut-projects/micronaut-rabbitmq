package io.micronaut.rabbitmq.annotation

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Body
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.reactivex.Completable
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

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
        boolean success = producer.goConfirm("def".bytes).blockingAwait(2, TimeUnit.SECONDS)

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
        void go(@Body byte[] data)

        @Binding("abc")
        Completable goConfirm(byte[] data)
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
