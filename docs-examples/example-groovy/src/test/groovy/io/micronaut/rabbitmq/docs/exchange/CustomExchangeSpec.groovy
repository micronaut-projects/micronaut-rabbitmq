package io.micronaut.rabbitmq.docs.exchange

import io.micronaut.rabbitmq.AbstractRabbitMQTest

class CustomExchangeSpec extends AbstractRabbitMQTest {

    void "test using a custom exchange"() {
        given:
        startContext()

        AnimalClient client = applicationContext.getBean(AnimalClient)
        AnimalListener listener = applicationContext.getBean(AnimalListener)

        when:
// tag::producer[]
        client.send(new Cat("Whiskers", 9))
        client.send(new Cat("Mr. Bigglesworth", 8))
        client.send(new Snake("Buttercup", false))
        client.send(new Snake("Monty the Python", true))
// end::producer[]

        then:
        waitFor {
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
    }
}
