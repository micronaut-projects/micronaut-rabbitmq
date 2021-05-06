package io.micronaut.rabbitmq.annotation

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import spock.util.concurrent.PollingConditions

class PropertyBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with rabbitmq properties"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        //https://www.rabbitmq.com/validated-user-id.html
        producer.go("property", "guest", new Person(name: "abc"))

        then:
        conditions.eventually {
            consumer.messages.size() == 1
            consumer.messages.keySet()[0].name == "abc"
            consumer.messages.values()[0] == "application/json|guest"
        }

        cleanup:
        applicationContext.close()
    }

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "PropertyBindingSpec")
    @RabbitClient
    static interface MyProducer {

        @RabbitProperty(name = "contentType", value = "application/json")
        void go(@Binding String binding, String userId, Person data)

    }

    @Requires(property = "spec.name", value = "PropertyBindingSpec")
    @RabbitListener
    static class MyConsumer {

        public static Map<Person, String> messages = [:]

        @Queue("property")
        void listen(Person data, String contentType, @RabbitProperty("userId") String user) {
            messages.put(data, contentType + '|' + user)
        }
    }
}
