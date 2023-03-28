package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Nullable
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class SimpleBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with a boolean"() {
        startContext()
        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go(true)
        producer.go(false)
        producer.go(null)

        then:
        waitFor {
            assert consumer.messages.size() == 3
            assert consumer.messages[0] == true
            assert consumer.messages[1] == false
            assert consumer.messages[2] == null
        }
    }

    void "test simple producing and consuming with a list of pojos"() {
        startContext()

        MyListProducer producer = applicationContext.getBean(MyListProducer)
        MyListConsumer consumer = applicationContext.getBean(MyListConsumer)

        when:
        producer.go([true, false])
        producer.go([null, true])

        then:
        waitFor {
            assert consumer.messages.size() == 4
            assert consumer.messages[0] == true
            assert consumer.messages[1] == false
            assert consumer.messages[2] == null
            assert consumer.messages[3] == true
        }
    }

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "SimpleBindingSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("simple")
        void go(Boolean data)
    }

    @Requires(property = "spec.name", value = "SimpleBindingSpec")
    @RabbitListener
    static class MyConsumer {

        static List<Boolean> messages = []

        @Queue("simple")
        void listen(@Nullable Boolean data) {
            messages << data
        }
    }

    @Requires(property = "spec.name", value = "SimpleBindingSpec")
    @RabbitClient
    static interface MyListProducer {
        @Binding("simple-list")
        void go(List<Boolean> data)
    }

    @Requires(property = "spec.name", value = "SimpleBindingSpec")
    @RabbitListener
    static class MyListConsumer {

        static List<Boolean> messages = []

        @Queue("simple-list")
        void listen(List<Boolean> data) {
            messages.addAll(data)
        }
    }
}
