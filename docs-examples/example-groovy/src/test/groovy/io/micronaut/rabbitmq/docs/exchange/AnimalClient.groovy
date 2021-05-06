package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.messaging.annotation.Header
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitClient("animals") // <1>
abstract class AnimalClient {

    abstract void send(@Header String animalType, Animal animal) // <2>

    void send(Animal animal) { // <3>
        send(animal.class.simpleName, animal)
    }
}
// end::clazz[]
