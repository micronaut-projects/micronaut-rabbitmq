package io.micronaut.rabbitmq.annotation

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import io.micronaut.serde.annotation.Serdeable

class HeaderExchangeSpec extends AbstractRabbitMQTest {

    void "test publishing and consuming on a topic exchange"() {

        startContext()

        AnimalProducer producer = applicationContext.getBean(AnimalProducer)
        AnimalListener consumer = applicationContext.getBean(AnimalListener)

        when:
        producer.go("Cat", new Cat(lives: 9, name: "Whiskers"))
        producer.go("Cat", new Cat(lives: 8, name: "Mr. Bigglesworth"))
        producer.go("Dog", new Dog(size: "M", name: "Chloe"))
        producer.go("Dog", new Dog(size: "L", name: "Butch"))

        then:
        waitFor {
            assert consumer.messages.size() == 4
            assert consumer.messages.find({ it.name == "Whiskers" }).lives == 9
            assert consumer.messages.find({ it.name == "Chloe" }).size == "M"
            assert consumer.messages.find({ it.name == "Mr. Bigglesworth" }).lives == 8
            assert consumer.messages.find({ it.name == "Butch" }).size == "L"
        }
    }

    @Serdeable
    static class Cat extends Animal {
        int lives
    }
    @Serdeable
    static class Dog extends Animal {
        String size
    }
    static abstract class Animal {
        String name
    }

    @Requires(property = "spec.name", value = "HeaderExchangeSpec")
    @RabbitClient("animals")
    static interface AnimalProducer {
        void go(@MessageHeader String animalType, Animal animal)
    }

    @Requires(property = "spec.name", value = "HeaderExchangeSpec")
    @RabbitListener
    static class AnimalListener {

        static List<Animal> messages = []

        @Queue("dogs")
        void listen(Dog dog) {
            messages << dog
        }

        @Queue("cats")
        void listen(Cat cat) {
            messages << cat
        }
    }
}
