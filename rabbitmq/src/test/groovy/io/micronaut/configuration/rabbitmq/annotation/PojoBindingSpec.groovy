package io.micronaut.configuration.rabbitmq.annotation

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import spock.util.concurrent.PollingConditions

class PojoBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with a pojo"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go(new Person(name: "abc"))

        then:
        conditions.eventually {
            consumer.messages.size() == 1
            consumer.messages[0].name == "abc"
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
        producer.go([new Person(name: "abc"), new Person(name: "def")])

        then:
        conditions.eventually {
            consumer.messages.size() == 2
            consumer.messages[0].name == "abc"
            consumer.messages[1].name == "def"
        }

        cleanup:
        applicationContext.close()
    }

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "PojoBindingSpec")
    @RabbitClient
    static interface MyProducer {

        @Binding("pojo")
        void go(Person data)

    }

    @Requires(property = "spec.name", value = "PojoBindingSpec")
    @RabbitListener
    static class MyConsumer {

        public static List<Person> messages = []

        @Queue("pojo")
        void listen(Person data) {
            messages.add(data)
        }
    }

    @Requires(property = "spec.name", value = "PojoBindingSpec")
    @RabbitClient
    static interface MyListProducer {

        @Binding("pojo-list")
        void go(List<Person> data)

    }

    @Requires(property = "spec.name", value = "PojoBindingSpec")
    @RabbitListener
    static class MyListConsumer {

        public static List<Person> messages = []

        @Queue("pojo-list")
        void listen(List<Person> data) {
            messages.addAll(data)
        }
    }
}
