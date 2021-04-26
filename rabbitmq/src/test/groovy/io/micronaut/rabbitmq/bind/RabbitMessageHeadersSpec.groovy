package io.micronaut.rabbitmq.bind

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.messaging.MessageHeaders
import io.micronaut.rabbitmq.annotation.Binding
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.bind.RabbitHeaders
import spock.util.concurrent.PollingConditions

class RabbitMessageHeadersSpec extends AbstractRabbitMQTest {

    void "test publishing and consuming with  MessageHeaders and RabbitHeaders"() {

        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 5)
        AnimalProducer producer = applicationContext.getBean(AnimalProducer)
        AnimalListener consumer = applicationContext.getBean(AnimalListener)

        when:
        producer.goWithMessageHeaders("Cat", new Cat(lives: 9, name: "Whiskers"), new RabbitHeaders(["color":"black"]))
        producer.goWithRabbitHeaders("Dog", new Dog(size: "M", name: "Chloe"), new RabbitHeaders(["weight":"2lbs"]))

        then:
        conditions.eventually {
            consumer.messages.size() == 2
            consumer.dogHeaders.size() == 1
            consumer.dogHeaders.get("weight") == "2lbs"
            consumer.catHeaders.size() == 1
            consumer.catHeaders.get("color") == "black"
        }

        cleanup:
        applicationContext.close()
    }

    static class Cat extends Animal {
        int lives
    }
    static class Dog extends Animal {
        String size
    }
    static abstract class Animal {
        String name
    }

    @Requires(property = "spec.name", value = "RabbitMessageHeadersSpec")
    @RabbitClient("animals")
    static interface AnimalProducer {
        void goWithMessageHeaders(@Header String animalType, Animal animal, MessageHeaders rabbitHeaders)
        void goWithRabbitHeaders(@Header String animalType, Animal animal, RabbitHeaders rabbitHeaders)
    }

    @Requires(property = "spec.name", value = "RabbitMessageHeadersSpec")
    @RabbitListener
    static class AnimalListener {

        public List<Animal> messages = []
        public Map<String, String> dogHeaders = [:]
        public Map<String, String> catHeaders = [:]

        @Queue("dogs")
        void listen(Dog dog, @Header String weight) {
            messages.add(dog)
            dogHeaders.put("weight", weight)
        }

        @Queue("cats")
        void listen(Cat cat, @Header String color) {
            messages.add(cat)
            catHeaders.put("color", color)
        }
    }
}
