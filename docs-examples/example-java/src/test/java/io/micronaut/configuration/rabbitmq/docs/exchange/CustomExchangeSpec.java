package io.micronaut.configuration.rabbitmq.docs.exchange;

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class CustomExchangeSpec extends AbstractRabbitMQTest {

    @Test
    void testUsingACustomExchange() {
        Map<String, Object> config = new HashMap<>();
        config.put("rabbitmq.port", rabbitContainer.getMappedPort(5672));
        config.put("spec.name", this.getClass().getSimpleName());
        ApplicationContext applicationContext = ApplicationContext.run(config, "test");

        AnimalClient client = applicationContext.getBean(AnimalClient.class);
        AnimalListener listener = applicationContext.getBean(AnimalListener.class);

        client.send("Cat", new Cat("Whiskers", 9));
        client.send("Cat", new Cat("Mr. Bigglesworth", 8));
        client.send("Snake", new Snake("Buttercup", false));
        client.send("Snake", new Snake("Monty the Python", true));
        Set<String> names = new HashSet<>();
        names.add("Whiskers");
        names.add("Mr. Bigglesworth");
        names.add("Buttercup");
        names.add("Monty the Python");

        await().atMost(10, SECONDS).until(() ->
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

        applicationContext.close();
    }
}
