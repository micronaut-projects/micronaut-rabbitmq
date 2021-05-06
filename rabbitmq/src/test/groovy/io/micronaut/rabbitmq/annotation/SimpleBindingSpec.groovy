package io.micronaut.rabbitmq.annotation

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Nullable
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import spock.util.concurrent.PollingConditions

class SimpleBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with a boolean"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go(true)
        producer.go(false)
        producer.go(null)

        then:
        conditions.eventually {
            consumer.messages.size() == 3
            consumer.messages[0] == true
            consumer.messages[1] == false
            consumer.messages[2] == null
        }

        cleanup:
        applicationContext.close()
    }

    void "test simple producing and consuming with a list of pojos"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyListProducer producer = applicationContext.getBean(MyListProducer)
        MyListConsumer consumer = applicationContext.getBean(MyListConsumer)

        when:
        producer.go([true, false])
        producer.go([null, true])

        then:
        conditions.eventually {
            consumer.messages.size() == 4
            consumer.messages[0] == true
            consumer.messages[1] == false
            consumer.messages[2] == null
            consumer.messages[3] == true
        }

        cleanup:
        applicationContext.close()
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

        public static List<Boolean> messages = []

        @Queue("simple")
        void listen(@Nullable Boolean data) {
            messages.add(data)
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

        public static List<Boolean> messages = []

        @Queue("simple-list")
        void listen(List<Boolean> data) {
            messages.addAll(data)
        }
    }
}
