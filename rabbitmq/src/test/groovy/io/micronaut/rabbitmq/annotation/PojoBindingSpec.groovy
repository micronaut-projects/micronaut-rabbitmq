package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class PojoBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with a pojo"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go(new Person(name: "abc"))

        then:
        waitFor {
            consumer.messages.size() == 1
            consumer.messages[0].name == "abc"
        }
    }

    void "test simple producing and consuming with a list of pojos"() {
        startContext()

        MyListProducer producer = applicationContext.getBean(MyListProducer)
        MyListConsumer consumer = applicationContext.getBean(MyListConsumer)

        when:
        producer.go([new Person(name: "abc"), new Person(name: "def")])

        then:
        waitFor {
            consumer.messages.size() == 2
            consumer.messages[0].name == "abc"
            consumer.messages[1].name == "def"
        }
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

        static List<Person> messages = []

        @Queue("pojo")
        void listen(Person data) {
            messages << data
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

        static List<Person> messages = []

        @Queue("pojo-list")
        void listen(List<Person> data) {
            messages.addAll(data)
        }
    }
}
