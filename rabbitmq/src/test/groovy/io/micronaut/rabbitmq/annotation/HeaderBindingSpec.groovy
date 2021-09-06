package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.AbstractRabbitMQTest

class HeaderBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with the header annotation"() {
        startContext()

        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        when:
        producer.go(new Person(name: "abc"), "some header")

        then:
        waitFor {
            consumer.messages.size() == 1
            consumer.messages.keySet()[0].name == "abc"
            consumer.messages.values()[0] == "some header|static header"
        }
    }

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "HeaderBindingSpec")
    @RabbitClient
    static interface MyProducer {
        @Binding("header")
        @MessageHeader(name = "static", value = "static header")
        void go(Person data, @MessageHeader String myHeader)
    }

    @Requires(property = "spec.name", value = "HeaderBindingSpec")
    @RabbitListener
    static class MyConsumer {

        static Map<Person, String> messages = [:]

        @Queue("header")
        void listen(Person data, @MessageHeader String myHeader,
                    @MessageHeader("static") String otherHeader) {
            messages[data] = myHeader + '|' + otherHeader
        }
    }
}
