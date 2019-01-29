package io.micronaut.configuration.rabbitmq.annotation

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import spock.util.concurrent.PollingConditions


class RabbitPojoTest extends AbstractRabbitMQTest {

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

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "RabbitPojoTest")
    @RabbitClient
    static interface MyProducer {

        @Binding("pojo")
        void go(Person data)

    }

    @Requires(property = "spec.name", value = "RabbitPojoTest")
    @RabbitListener
    static class MyConsumer {

        public static List<Person> messages = []

        @Queue("pojo")
        void listen(Person data) {
            messages.add(data)
        }
    }
}
