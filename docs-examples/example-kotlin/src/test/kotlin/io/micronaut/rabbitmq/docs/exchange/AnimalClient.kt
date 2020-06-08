package io.micronaut.rabbitmq.docs.exchange

// tag::imports[]
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.annotation.Header
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitClient("animals") // <1>
abstract class AnimalClient {

    abstract fun send(@Header animalType: String, animal: Animal)  // <2>

    fun send(animal: Animal) { //<3>
        send(animal.javaClass.simpleName, animal)
    }
}
// end::clazz[]