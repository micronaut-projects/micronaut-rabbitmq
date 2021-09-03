package io.micronaut.rabbitmq.listener

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArraySet

class MultipleConsumersSpec extends AbstractRabbitMQTest {

    void "test multiple consumers"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        4.times { producer.go("abc") }

        then:
        waitFor {
            //size check because container is set, so 5 different threads are used.
            consumer.threads.size() == 4
        }
    }

    @Requires(property = "spec.name", value = "MultipleConsumersSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("simple")
        void go(String data)
    }

    @Requires(property = "spec.name", value = "MultipleConsumersSpec")
    @RabbitListener
    static class MyConsumer {

        CopyOnWriteArraySet<String> threads = new CopyOnWriteArraySet<>()

        @Queue(value = "simple", numberOfConsumers = 4)
        void listen(@MessageBody String body) {
            threads << Thread.currentThread().name
            sleep 500
        }
    }
}
