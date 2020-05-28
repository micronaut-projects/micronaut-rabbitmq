package io.micronaut.rabbitmq.docs.exchange

// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.context.annotation.Requires
import java.util.*

// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitListener // <1>
class AnimalListener {

    val receivedAnimals: MutableList<Animal> = Collections.synchronizedList(ArrayList())

    @Queue("cats") // <2>
    fun receive(cat: Cat) { // <3>
        receivedAnimals.add(cat)
    }

    @Queue("snakes") // <2>
    fun receive(snake: Snake) { // <3>
        receivedAnimals.add(snake)
    }
}
// end::clazz[]
