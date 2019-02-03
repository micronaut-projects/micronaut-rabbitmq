package io.micronaut.configuration.rabbitmq.docs.exchange

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::class[]
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
// end::class[]
