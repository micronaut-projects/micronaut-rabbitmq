package io.micronaut.rabbitmq.annotation

import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Envelope
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import spock.util.concurrent.PollingConditions

import java.util.concurrent.atomic.AtomicInteger

class TypeBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with rabbitmq properties"() {
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go(4)

        then:
        conditions.eventually {
            consumer.messages.get() == 4
        }

        cleanup:
        applicationContext.close()
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

        public static AtomicInteger messages = new AtomicInteger()

        @Queue("type")
        void listen(Integer data, Envelope envelope, BasicProperties basicProperties) {
            assert envelope != null
            assert basicProperties != null
            messages.updateAndGet({l -> l + data})
        }
    }
}
