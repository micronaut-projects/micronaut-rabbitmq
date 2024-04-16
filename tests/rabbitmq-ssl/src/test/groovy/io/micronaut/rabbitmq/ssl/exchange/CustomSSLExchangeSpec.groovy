package io.micronaut.rabbitmq.ssl.exchange

import io.micronaut.context.annotation.Property
import io.micronaut.rabbitmq.ssl.AbstractRabbitMQSSLTest

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await

@Property(name = "spec.name", value = "CustomSSLExchangeSpec")
class CustomSSLExchangeSpec extends AbstractRabbitMQSSLTest {

    void "test using a custom exchange"() {
        given:
        startContext()
        AnimalClient client = applicationContext.getBean(AnimalClient)
        AnimalListener listener = applicationContext.getBean(AnimalListener)

        when:
        client.send(new Cat("Whiskers", 9))
        client.send(new Cat("Mr. Bigglesworth", 8))
        client.send(new Snake("Buttercup", false))
        client.send(new Snake("Monty the Python", true))
        await().atMost(10, SECONDS).until {
            listener.receivedAnimals.size() == 4
        }

        then:
        assert listener.receivedAnimals.size() == 4

        assert listener.receivedAnimals.find({ animal ->
            animal instanceof Cat && animal.name == "Whiskers" && ((Cat) animal).lives == 9
        }) != null

        assert listener.receivedAnimals.find({ animal ->
            animal instanceof Cat && animal.name == "Mr. Bigglesworth" && ((Cat) animal).lives == 8
        }) != null

        assert listener.receivedAnimals.find({ animal ->
            animal instanceof Snake && animal.name == "Buttercup" && !((Snake) animal).venomous
        }) != null

        assert listener.receivedAnimals.find({ animal ->
            animal instanceof Snake && animal.name == "Monty the Python" && ((Snake) animal).venomous
        }) != null
    }
}
