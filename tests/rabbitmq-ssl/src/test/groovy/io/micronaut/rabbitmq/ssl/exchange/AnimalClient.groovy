package io.micronaut.rabbitmq.ssl.exchange

import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.RabbitClient

@Requires(property = "spec.name", value = "CustomSSLExchangeSpec")
@RabbitClient("animals")
abstract class AnimalClient {

    abstract void send(@MessageHeader String animalType, Animal animal)

    void send(Animal animal) {
        send(animal.getClass().simpleName, animal)
    }
}
