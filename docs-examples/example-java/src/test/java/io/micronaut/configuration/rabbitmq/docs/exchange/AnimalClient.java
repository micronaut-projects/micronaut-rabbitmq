package io.micronaut.configuration.rabbitmq.docs.exchange;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.RabbitClient;
import io.micronaut.context.annotation.Requires;
import io.micronaut.messaging.annotation.Header;
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitClient("animals") // <1>
public interface AnimalClient {

    void send(@Header String animalType, Animal animal); // <2>

    default void send(Animal animal) { // <3>
        send(animal.getClass().getSimpleName(), animal);
    }
}
// end::clazz[]