package io.micronaut.configuration.rabbitmq.annotation

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import spock.util.concurrent.PollingConditions

class RabbitHeaderTest extends AbstractRabbitMQTest {

    void "test simple producing and consuming with the header annotation"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go(new Person(name: "abc"), "some header")

        then:
        conditions.eventually {
            consumer.messages.size() == 1
            consumer.messages.keySet()[0].name == "abc"
            consumer.messages.values()[0] == "some header|static header"
        }

        cleanup:
        applicationContext.close()
    }

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "RabbitHeaderTest")
    @RabbitClient
    static interface MyProducer {

        @Binding("header")
        @Header(name = "static", value = "static header")
        void go(Person data, @Header String myHeader)

    }

    @Requires(property = "spec.name", value = "RabbitHeaderTest")
    @RabbitListener
    static class MyConsumer {

        public static Map<Person, String> messages = [:]

        @Queue("header")
        void listen(Person data, @Header String myHeader, @Header("static") String otherHeader) {
            messages.put(data, myHeader + '|' + otherHeader)
        }
    }
}
