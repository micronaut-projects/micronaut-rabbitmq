package io.micronaut.rabbitmq

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import io.micronaut.messaging.MessageHeaders
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import spock.util.concurrent.PollingConditions

class RabbitHeadersSpec extends AbstractRabbitMQTest {

    void "test simple producing and consuming with the MessageHeaders parameter"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": AbstractRabbitMQTest.rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducerWithMessageHeaders producer = applicationContext.getBean(MyProducerWithMessageHeaders)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        Map<String, String> headerMap = new HashMap()
        headerMap.put("weight", "200lbs")
        headerMap.put("height", "6ft")
        RabbitHeaders rabbitHeaders = new RabbitHeaders(headerMap)

        when:
        producer.go(new Person(name: "abc"), rabbitHeaders)

        then:
        conditions.eventually {
            consumer.person.name == "abc"
            consumer.headers.keySet()[0] == "height"
            consumer.headers.values()[0] == "6ft"
            consumer.headers.keySet()[1] == "weight"
            consumer.headers.values()[1] == "200lbs"

        }

        cleanup:
        applicationContext.close()
    }

    void "test simple producing and consuming with the RabbitHeaders parameter"() {
        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": AbstractRabbitMQTest.rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 3)
        MyProducerWithRabbitHeaders producer = applicationContext.getBean(MyProducerWithRabbitHeaders)
        MyConsumer consumer = applicationContext.getBean(MyConsumer)

        Map<String, String> headerMap = new HashMap();
        headerMap.put("weight", "200lbs")
        headerMap.put("height", "6ft")
        RabbitHeaders rabbitHeaders = new RabbitHeaders(headerMap)

        when:
        producer.go(new Person(name: "abc"), rabbitHeaders)

        then:
        conditions.eventually {
            consumer.person.name == "abc"
            consumer.headers.keySet()[0] == "height"
            consumer.headers.values()[0] == "6ft"
            consumer.headers.keySet()[1] == "weight"
            consumer.headers.values()[1] == "200lbs"

        }

        cleanup:
        applicationContext.close()
    }

    static class Person {
        String name
    }

    @Requires(property = "spec.name", value = "RabbitHeadersSpec")
    @RabbitClient
    static interface MyProducerWithMessageHeaders {

        @Binding("header")
        void go(Person data, MessageHeaders messageHeaders)

    }

    @Requires(property = "spec.name", value = "RabbitHeadersSpec")
    @RabbitClient
    static interface MyProducerWithRabbitHeaders {

        @Binding("header")
        void go(Person data, RabbitHeaders rabbitHeaders)

    }

    @Requires(property = "spec.name", value = "RabbitHeadersSpec")
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
