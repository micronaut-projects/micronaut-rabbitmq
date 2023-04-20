package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await


@MicronautTest
@Property(name = "spec.name", value = "CustomExchangeSpec")
class CustomExchangeSpec extends Specification {

    @Inject AnimalClient client
    @Inject AnimalListener listener

    void "test using a custom exchange"() {
        when:
// tag::producer[]
        client.send(new Cat("Whiskers", 9))
        client.send(new Cat("Mr. Bigglesworth", 8))
        client.send(new Snake("Buttercup", false))
        client.send(new Snake("Monty the Python", true))
// end::producer[]
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
