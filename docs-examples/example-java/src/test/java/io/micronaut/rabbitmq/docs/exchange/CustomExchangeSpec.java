package io.micronaut.rabbitmq.docs.exchange;

import io.micronaut.context.ApplicationContext;
import io.micronaut.rabbitmq.AbstractRabbitMQTest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class CustomExchangeSpec extends AbstractRabbitMQTest {

    @Test
    void testUsingACustomExchange() {
        ApplicationContext applicationContext = startContext();

        AnimalClient client = applicationContext.getBean(AnimalClient.class);
        AnimalListener listener = applicationContext.getBean(AnimalListener.class);

        client.send(new Cat("Whiskers", 9));
        client.send(new Cat("Mr. Bigglesworth", 8));
        client.send(new Snake("Buttercup", false));
        client.send(new Snake("Monty the Python", true));

        try {
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
        } finally {
            applicationContext.close();
        }
    }
}
