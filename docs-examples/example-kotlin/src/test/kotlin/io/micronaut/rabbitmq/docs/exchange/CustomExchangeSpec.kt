package io.micronaut.rabbitmq.docs.exchange

import io.kotlintest.eventually
import io.kotlintest.matchers.collections.shouldExist
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micronaut.rabbitmq.AbstractRabbitMQTest

import org.opentest4j.AssertionFailedError

class CustomExchangeSpec: AbstractRabbitMQTest({

    val specName = javaClass.simpleName

    given("Using a custom exchange") {
        val ctx = startContext(specName)

        val client = ctx.getBean(AnimalClient::class.java)
        val listener = ctx.getBean(AnimalListener::class.java)

        `when`("the messages are published") {
            client.send(Cat("Whiskers", 9))
            client.send(Cat("Mr. Bigglesworth", 8))
            client.send(Snake("Buttercup", false))
            client.send(Snake("Monty the Python", true))

            then("the messages are received") {
                val messages = listener.receivedAnimals
                eventually(10.seconds, AssertionFailedError::class.java) {
                    messages.size shouldBe 4
                    messages shouldExist({ animal: Animal ->
                        Cat::class.isInstance(animal) && animal.name == "Whiskers"
                    })
                    messages shouldExist({ animal: Animal ->
                        Cat::class.isInstance(animal) && animal.name == "Mr. Bigglesworth"
                    })
                    messages shouldExist({ animal: Animal ->
                        Snake::class.isInstance(animal) && animal.name == "Buttercup"
                    })
                    messages shouldExist({ animal: Animal ->
                        Snake::class.isInstance(animal) && animal.name == "Monty the Python"
                    })
                }
            }
        }

        ctx.stop()
    }
})
