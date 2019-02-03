package io.micronaut.configuration.rabbitmq.docs.exchange;

import io.micronaut.configuration.rabbitmq.AbstractRabbitMQTest
import io.micronaut.context.ApplicationContext
import spock.util.concurrent.PollingConditions

class CustomExchangeSpec extends AbstractRabbitMQTest {

    void "test using a custom exchange"() {
        given:
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)

        AnimalClient client = applicationContext.getBean(AnimalClient.class)
        AnimalListener listener = applicationContext.getBean(AnimalListener.class)

        when:
// tag::producer[]
        client.send("Cat", new Cat("Whiskers", 9))
        client.send("Cat", new Cat("Mr. Bigglesworth", 8))
        client.send("Snake", new Snake("Buttercup", false))
        client.send("Snake", new Snake("Monty the Python", true))
// end::producer[]

        then:
        conditions.eventually {
            listener.receivedAnimals.size() == 4
            listener.receivedAnimals.find({ animal ->
                animal instanceof Cat && animal.name == "Whiskers" && ((Cat) animal).lives == 9
            }) != null
            listener.receivedAnimals.find({ animal ->
                animal instanceof Cat && animal.name == "Mr. Bigglesworth" && ((Cat) animal).lives == 8
            }) != null
            listener.receivedAnimals.find({ animal ->
                animal instanceof Snake && animal.name == "Buttercup" && !((Snake) animal).venomous
            }) != null
            listener.receivedAnimals.find({ animal ->
                animal instanceof Snake && animal.name == "Monty the Python" && ((Snake) animal).venomous
            }) != null
        }

        cleanup:
        applicationContext.close()
    }
}
