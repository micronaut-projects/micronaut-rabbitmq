package io.micronaut.rabbitmq.listener

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageBody
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArraySet

class MultipleConsumersSpec extends AbstractRabbitMQTest {

    void "test multiple consumers"() {
        ApplicationContext ctx = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)
        MyProducer producer = ctx.getBean(MyProducer)
        MyConsumer consumer = ctx.getBean(MyConsumer)

        when:
        5.times { producer.go("abc") }

        then:
        conditions.eventually {
            //size check because container is set, so 5 different threads are used.
            assert consumer.threads.size() == 5
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

        @Queue(value = "simple", numberOfConsumers = 5)
        void listen(@MessageBody String body) {
            threads.add(Thread.currentThread().getName())
            Thread.sleep(500)
        }
    }
}
