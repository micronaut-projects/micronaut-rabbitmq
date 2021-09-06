package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener

import java.util.concurrent.CopyOnWriteArrayList
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitListener // <1>
class AnimalListener {

    CopyOnWriteArrayList<Animal> receivedAnimals = []

    @Queue("cats") // <2>
    void receive(Cat cat) { // <3>
        receivedAnimals << cat
    }

    @Queue("snakes") // <2>
    void receive(Snake snake) { // <3>
        receivedAnimals << snake
    }
}
// end::clazz[]
