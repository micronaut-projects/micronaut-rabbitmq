package io.micronaut.rabbitmq.annotation

import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Envelope
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest

import java.util.concurrent.atomic.AtomicInteger

class TypeBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with rabbitmq properties"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go(4)

        then:
        waitFor {
            assert consumer.messages.get() == 4
        }
    }

    @Requires(property = "spec.name", value = "TypeBindingSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("type")
        void go(Integer data)
    }

    @Requires(property = "spec.name", value = "TypeBindingSpec")
    @RabbitListener
    static class MyConsumer {

        static AtomicInteger messages = new AtomicInteger()

        @Queue("type")
        void listen(Integer data, Envelope envelope, BasicProperties basicProperties) {
            assert envelope != null
            assert basicProperties != null
            messages.updateAndGet({l -> l + data})
        }
    }
}
