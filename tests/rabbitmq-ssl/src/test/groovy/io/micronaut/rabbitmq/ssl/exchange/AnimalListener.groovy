package io.micronaut.rabbitmq.ssl.exchange

import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.annotation.Queue
import io.micronaut.rabbitmq.annotation.RabbitListener
import java.util.concurrent.CopyOnWriteArrayList

@Requires(property = "spec.name", value = "CustomSSLExchangeSpec")
@RabbitListener
class AnimalListener {

    CopyOnWriteArrayList<Animal> receivedAnimals = []

    @Queue("cats")
    void receive(Cat cat) {
        receivedAnimals << cat
    }

    @Queue("snakes")
    void receive(Snake snake) {
        receivedAnimals << snake
    }
}
