package io.micronaut.rabbitmq.docs.exchange;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.RabbitClient;
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitClient("animals") // <1>
public interface AnimalClient {

    void send(@MessageHeader String animalType, Animal animal); // <2>

    default void send(Animal animal) { // <3>
        send(animal.getClass().getSimpleName(), animal);
    }
}
// end::clazz[]
