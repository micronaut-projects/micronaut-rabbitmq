package io.micronaut.rabbitmq.docs.exchange

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitListener // <1>
class AnimalListener {

    List<Animal> receivedAnimals = Collections.synchronizedList([])

    @Queue("cats") // <2>
    void receive(Cat cat) { // <3>
        receivedAnimals.add(cat)
    }

    @Queue("snakes") // <2>
    void receive(Snake snake) { // <3>
        receivedAnimals.add(snake)
    }
}
// end::clazz[]
