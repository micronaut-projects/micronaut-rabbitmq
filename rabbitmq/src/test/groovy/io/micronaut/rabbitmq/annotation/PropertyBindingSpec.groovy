package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.serde.annotation.Serdeable

class PropertyBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with rabbitmq properties"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        //https://www.rabbitmq.com/validated-user-id.html
        producer.go("property", "guest", new Person(name: "abc"))

        then:
        waitFor {
            assert consumer.messages.size() == 1
            assert consumer.messages.keySet()[0].name == "abc"
            assert consumer.messages.values()[0] == "application/json|guest"
        }
    }

    @Serdeable
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

        static Map<Person, String> messages = [:]

        @Queue("property")
        void listen(Person data, String contentType, @RabbitProperty("userId") String user) {
            messages[data] = contentType + '|' + user
        }
    }
}
