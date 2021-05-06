package io.micronaut.rabbitmq.docs.exchange;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// end::imports[]

@Requires(property = "spec.name", value = "CustomExchangeSpec")
// tag::clazz[]
@RabbitListener // <1>
public class AnimalListener {

    List<Animal> receivedAnimals = Collections.synchronizedList(new ArrayList<>());

    @Queue("cats") // <2>
    public void receive(Cat cat) { // <3>
        receivedAnimals.add(cat);
    }

    @Queue("snakes") // <2>
    public void receive(Snake snake) { // <3>
        receivedAnimals.add(snake);
    }
}
// end::clazz[]
