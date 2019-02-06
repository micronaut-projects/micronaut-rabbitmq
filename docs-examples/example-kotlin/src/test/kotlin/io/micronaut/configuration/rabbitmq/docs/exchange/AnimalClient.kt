package io.micronaut.configuration.rabbitmq.docs.exchange

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitClient("animals") // <1>
abstract class AnimalClient {

    abstract fun send(@Header animalType: String, animal: Animal)  // <3>

    fun send(animal: Animal) {
        send(animal.javaClass.simpleName, animal)
    }
}
// end::clazz[]