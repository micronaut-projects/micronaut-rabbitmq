package io.micronaut.configuration.rabbitmq

import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.configuration.rabbitmq.annotation.Binding
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Body
import io.reactivex.Completable
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

class RabbitClientTest extends Specification {

    @Shared
    @AutoCleanup
    GenericContainer rabbitContainer =
            new GenericContainer("library/rabbitmq:3.7")
                    .withExposedPorts(5672)
                    .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*Server startup complete.*"))

    void "test simple producing and consuming"() {
        rabbitContainer.start()
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
    }

    @Requires(property = "spec.name", value = "RabbitClientTest")
    @RabbitClient
    static interface MyProducer {

        @Binding("abc")
        void go(@Body byte[] data)

        @Binding("abc")
        Completable goConfirm(@Body byte[] data)
    }

    @Requires(property = "spec.name", value = "RabbitClientTest")
    @RabbitListener
    static class MyConsumer {

        public static List<byte[]> messages = []

        @Queue("abc")
        void listen(byte[] data) {
            messages.add(data)
        }
    }
}
