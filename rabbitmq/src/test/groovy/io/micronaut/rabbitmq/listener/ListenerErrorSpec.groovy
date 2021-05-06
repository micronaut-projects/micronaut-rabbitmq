package io.micronaut.rabbitmq.listener

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.exception.DefaultRabbitListenerExceptionHandler
import io.micronaut.rabbitmq.exception.RabbitListenerException
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler
import spock.util.concurrent.PollingConditions

import javax.inject.Singleton

class ListenerErrorSpec extends AbstractRabbitMQTest {

    void "test a local error handler"() {
        ApplicationContext ctx = startContext([global: false])
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = ctx.getBean(MyProducer)
        producer.go("abc")
        producer.go("def")
        producer.go("ghi")

        when:
        MyConsumer consumer = ctx.getBean(MyConsumer)

        then:
        conditions.eventually {
            consumer.errors.size() == 3
            consumer.errors.collect { it.cause.message } as Set == ["abc", "def", "ghi"] as Set
        }

        cleanup:
        ctx.close()
    }

    void "test a global error handler"() {
        ApplicationContext ctx = startContext([global: true])
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = ctx.getBean(MyProducer)
        producer.go("abc")
        producer.go("def")
        producer.go("ghi")

        when:
        MyGlobalErrorHandler handler = ctx.getBean(MyGlobalErrorHandler)

        then:
        conditions.eventually {
            handler.errors.size() == 3
            handler.errors.collect { it.cause.message } as Set == ["abc", "def", "ghi"] as Set
        }

        cleanup:
        ctx.close()
    }

    @Requires(property = "spec.name", value = "ListenerErrorSpec")
    @RabbitClient
    static interface MyProducer {

        @Binding("simple")
        void go(String data)

    }

    @Requires(property = "spec.name", value = "ListenerErrorSpec")
    @Requires(property = "global", value = "false")
    @RabbitListener
    static class MyConsumer implements RabbitListenerExceptionHandler {

        public static List<Throwable> errors = []

        @Queue("simple")
        void listen(String data) {
            throw new RuntimeException(data)
        }

        @Override
        void handle(RabbitListenerException exception) {
            errors.add(exception)
        }
    }

    @Requires(property = "spec.name", value = "ListenerErrorSpec")
    @Requires(property = "global", value = "true")
    @RabbitListener
    static class MyConsumer2 {

        @Queue("simple")
        void listen(String data) {
            throw new RuntimeException(data)
        }
    }

    @Requires(property = "spec.name", value = "ListenerErrorSpec")
    @Requires(property = "global", value = "true")
    @Replaces(DefaultRabbitListenerExceptionHandler)
    @Primary
    @Singleton
    static class MyGlobalErrorHandler implements RabbitListenerExceptionHandler {

        public static List<Throwable> errors = []

        @Override
        void handle(RabbitListenerException exception) {
            errors.add(exception)
        }
    }
}
