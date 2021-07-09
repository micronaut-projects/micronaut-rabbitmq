package io.micronaut.rabbitmq.annotation

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.AbstractRabbitMQTest
import spock.util.concurrent.PollingConditions

class HeaderExchangeSpec extends AbstractRabbitMQTest {

    void "test publishing and consuming on a topic exchange"() {

        ApplicationContext applicationContext = ApplicationContext.run(
                ["rabbitmq.port": rabbitContainer.getMappedPort(5672),
                 "spec.name": getClass().simpleName], "test")
        PollingConditions conditions = new PollingConditions(timeout: 10)
        AnimalProducer producer = applicationContext.getBean(AnimalProducer)
        AnimalListener consumer = applicationContext.getBean(AnimalListener)

        when:
        producer.go("Cat", new Cat(lives: 9, name: "Whiskers"))
        producer.go("Cat", new Cat(lives: 8, name: "Mr. Bigglesworth"))
        producer.go("Dog", new Dog(size: "M", name: "Chloe"))
        producer.go("Dog", new Dog(size: "L", name: "Butch"))

        then:
        conditions.eventually {
            consumer.messages.size() == 4
            consumer.messages.find({ it.name == "Whiskers" }).lives == 9
            consumer.messages.find({ it.name == "Chloe" }).size == "M"
            consumer.messages.find({ it.name == "Mr. Bigglesworth" }).lives == 8
            consumer.messages.find({ it.name == "Butch" }).size == "L"
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

    @Requires(property = "spec.name", value = "HeaderExchangeSpec")
    @RabbitClient("animals")
    static interface AnimalProducer {

        void go(@MessageHeader String animalType, Animal animal)
    }

    @Requires(property = "spec.name", value = "HeaderExchangeSpec")
    @RabbitListener
    static class AnimalListener {

        public static List<Animal> messages = []

        @Queue("dogs")
        void listen(Dog dog) {
            messages.add(dog)
        }

        @Queue("cats")
        void listen(Cat cat) {
            messages.add(cat)
        }
    }
}
