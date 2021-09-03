package io.micronaut.rabbitmq.listener

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
import jakarta.inject.Singleton

import java.util.concurrent.CopyOnWriteArrayList

class ListenerErrorSpec extends AbstractRabbitMQTest {

    void "test a local error handler"() {
        startContext(global: false)
        MyProducer producer = applicationContext.getBean(MyProducer)
        producer.go("abc")
        producer.go("def")
        producer.go("ghi")

        when:
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        then:
        waitFor {
            consumer.errors.size() == 3
            consumer.errors.collect { it.cause.message } as Set == ["abc", "def", "ghi"] as Set
        }
    }

    void "test a global error handler"() {
        startContext(global: true)
        MyProducer producer = applicationContext.getBean(MyProducer)
        producer.go("abc")
        producer.go("def")
        producer.go("ghi")

        when:
        MyGlobalErrorHandler handler = applicationContext.getBean(MyGlobalErrorHandler)

        then:
        waitFor {
            handler.errors.size() == 3
            handler.errors.collect { it.cause.message } as Set == ["abc", "def", "ghi"] as Set
        }
    }

    @Requires(property = "spec.name", value = "ListenerErrorSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("error")
        void go(String data)
    }

    @Requires(property = "spec.name", value = "ListenerErrorSpec")
    @Requires(property = "global", value = "false")
    @RabbitListener
    static class MyConsumer implements RabbitListenerExceptionHandler {

        CopyOnWriteArrayList<Throwable> errors = []

        @Queue("error")
        void listen(String data) {
            throw new RuntimeException(data)
        }

        @Override
        void handle(RabbitListenerException e) {
            errors << e
        }
    }

    @Requires(property = "spec.name", value = "ListenerErrorSpec")
    @Requires(property = "global", value = "true")
    @RabbitListener
    static class MyConsumer2 {

        @Queue("error")
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

        CopyOnWriteArrayList<Throwable> errors = []

        @Override
        void handle(RabbitListenerException e) {
            errors << e
        }
    }
}
