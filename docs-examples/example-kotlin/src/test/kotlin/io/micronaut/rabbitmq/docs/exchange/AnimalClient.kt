package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.context.annotation.Requires
// tag::imports[]
import io.micronaut.messaging.annotation.MessageHeader
import io.micronaut.rabbitmq.annotation.RabbitClient
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitClient("animals") // <1>
abstract class AnimalClient {

    abstract fun send(@MessageHeader animalType: String, animal: Animal)  // <2>

    fun send(animal: Animal) { //<3>
        send(animal.javaClass.simpleName, animal)
    }
}
// end::clazz[]
