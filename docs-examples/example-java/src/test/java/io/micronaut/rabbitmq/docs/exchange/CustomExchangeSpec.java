package io.micronaut.rabbitmq.docs.exchange;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import io.micronaut.rabbitmq.testcontainers.RabbitMQ;

@MicronautTest
@Property(name = "spec.name", value = "CustomExchangeSpec")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomExchangeSpec implements TestPropertyProvider {
    @Override
    public Map<String, String> getProperties() {
        return RabbitMQ.getProperties();
    }

    @Test
    void testUsingACustomExchange(AnimalClient client, AnimalListener listener) {

        client.send(new Cat("Whiskers", 9));
        client.send(new Cat("Mr. Bigglesworth", 8));
        client.send(new Snake("Buttercup", false));
        client.send(new Snake("Monty the Python", true));

        await().atMost(60, SECONDS).until(() ->
                listener.receivedAnimals.size() == 4 &&
                listener.receivedAnimals.stream()
                        .filter(Cat.class::isInstance)
                        .anyMatch(cat -> cat.getName().equals("Whiskers") && ((Cat) cat).getLives() == 9) &&
                listener.receivedAnimals.stream()
                        .filter(Cat.class::isInstance)
                        .anyMatch(cat -> cat.getName().equals("Mr. Bigglesworth") && ((Cat) cat).getLives() == 8) &&
                listener.receivedAnimals.stream()
                        .filter(Snake.class::isInstance)
                        .anyMatch(snake -> snake.getName().equals("Buttercup") && !((Snake) snake).isVenomous()) &&
                listener.receivedAnimals.stream()
                        .filter(Snake.class::isInstance)
                        .anyMatch(snake -> snake.getName().equals("Monty the Python") && ((Snake) snake).isVenomous())
        );
    }
}
