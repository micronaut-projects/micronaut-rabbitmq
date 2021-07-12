package io.micronaut.rabbitmq.listener

import io.micronaut.context.ApplicationContext
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
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList

class MissingHeaderSpec extends AbstractRabbitMQTest {

    void "test a null header value"() {
        ApplicationContext ctx = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = ctx.getBean(MyProducer)
        producer.go("abc")

        when:
        MyConsumer consumer = ctx.getBean(MyConsumer)

        then:
        conditions.eventually {
            consumer.headers.size() == 0
            consumer.errors.size() == 0
            consumer.bodies.size() == 1
        }

        cleanup:
        ctx.close()
    }

    @Requires(property = "spec.name", value = "MissingHeaderSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("simple")
        void go(String data)
    }

    @Requires(property = "spec.name", value = "MissingHeaderSpec")
    @RabbitListener
    static class MyConsumer implements RabbitListenerExceptionHandler {

        public static CopyOnWriteArrayList<String> headers = []
        public static CopyOnWriteArrayList<RabbitListenerException> errors = []
        public static CopyOnWriteArrayList<String> bodies = []

        @Queue("simple")
        void listen(@Nullable @MessageHeader("X-Header") String header, String data) {
            if (header != null) headers.add(header)
            bodies.add(data)
        }

        @Override
        void handle(RabbitListenerException exception) {
            errors.add(exception)
            exception.printStackTrace()
        }
    }
}
