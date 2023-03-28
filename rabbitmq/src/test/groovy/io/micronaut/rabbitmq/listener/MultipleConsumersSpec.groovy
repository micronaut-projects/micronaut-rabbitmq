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
        4.times { producer.go("someData") }

        then:
        waitFor {
            //size check because container is set, so 5 different threads are used.
            assert consumer.threads.size() == 4
        }
    }

    @Override
    protected void startContext(Map additionalConfig) {
        additionalConfig['rabbitmq.simple-queue.number-of-consumers'] = 3
        super.startContext(additionalConfig)
    }

    void "test multiple consumers using dynamic configuration"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyNewConsumer consumer = applicationContext.getBean(MyNewConsumer)

        when:
        4.times { producer.go("someData") }

        then:
        waitFor {
            //size check because container is set, so 5 different threads are used.
            assert consumer.threads.size() == 3
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
        void listenOne(@MessageBody String body) {
            threads << Thread.currentThread().name
            sleep 500
        }
    }

    @Requires(property = "spec.name", value = "MultipleConsumersSpec")
    @RabbitListener
    static class MyNewConsumer {

        CopyOnWriteArraySet<String> threads = new CopyOnWriteArraySet<>()

        @Queue(value = 'simple', numberOfConsumersValue = '${rabbitmq.simple-queue.number-of-consumers}')
        void listenTwo(@MessageBody String body) {
            threads << Thread.currentThread().name
            sleep 500
        }
    }
}
