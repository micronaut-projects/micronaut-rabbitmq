package io.micronaut.rabbitmq.listener

import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Nullable
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.exception.RabbitListenerException
import io.micronaut.rabbitmq.exception.RabbitListenerExceptionHandler

import java.util.concurrent.CopyOnWriteArrayList

class MissingHeaderSpec extends AbstractRabbitMQTest {

    void "test a null header value"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        producer.go("abc")

        when:
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        then:
        waitFor {
            assert consumer.headers.size() == 0
            assert consumer.errors.size() == 0
            assert consumer.bodies.size() == 1
        }
    }

    @Requires(property = "spec.name", value = "MissingHeaderSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("simple-header")
        void go(String data)
    }

    @Requires(property = "spec.name", value = "MissingHeaderSpec")
    @RabbitListener
    static class MyConsumer implements RabbitListenerExceptionHandler {

        static CopyOnWriteArrayList<String> headers = []
        static CopyOnWriteArrayList<RabbitListenerException> errors = []
        static CopyOnWriteArrayList<String> bodies = []

        @Queue("simple-header")
        void listen(@Nullable @MessageHeader("X-Header") String header, String data) {
            if (header != null) headers << header
            bodies << data
        }

        @Override
        void handle(RabbitListenerException e) {
            errors << e
            e.printStackTrace()
        }
    }
}
