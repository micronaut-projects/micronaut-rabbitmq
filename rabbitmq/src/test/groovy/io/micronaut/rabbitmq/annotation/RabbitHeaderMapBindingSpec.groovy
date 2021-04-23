package io.micronaut.rabbitmq.annotation

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.messaging.MessageHeaders;
import io.micronaut.rabbitmq.RabbitHeaders;
import spock.util.concurrent.PollingConditions

class RabbitHeaderMapBindingSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with the header annotation"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducer producer = applicationContext.getBean(MyProducer)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        Map<String, String> headerMap = new HashMap();
        headerMap.put("weight", "200lbs");
        headerMap.put("height", "6ft");
        RabbitHeaders rabbitHeaders = new RabbitHeaders(headerMap);

        when:
        producer.go(new Person(name: "abc"), rabbitHeaders)

        then:
        conditions.eventually {
            consumer.person.name() == "abc"
            consumer.headers.keySet()[0] == "weight"
            consumer.headers.values()[0] == "200lbs"
            consumer.headers.keySet()[1] == "height"
            consumer.headers.values()[1] == "6ft"
        }

        cleanup:
        applicationContext.close()
    }

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "HeaderBindingSpec")
    @RabbitClient
    static interface MyProducer {

        @Binding("header")
        void go(Person data, RabbitHeaders rabbitHeaders)

    }

    @Requires(property = "spec.name", value = "HeaderBindingSpec")
    @RabbitListener
    static class MyConsumer {

        public static Map<String, String> headers = [:]
        public static Person person

        @Queue("header")
        void listen(Person data, @Header String height, @Header("weight") String weight) {
            person = data
            headers.put("height", height)
            headers.put("weight", weight)
        }
    }
}
