package io.micronaut.rabbitmq.docs.exchange

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@MicronautTest
@Property(name = "spec.name", value = "CustomExchangeSpec")
class CustomExchangeSpec(client: AnimalClient, listener: AnimalListener): BehaviorSpec({

    val specName = javaClass.simpleName

    given("Using a custom exchange") {
        `when`("the messages are published") {
            client.send(Cat("Whiskers", 9))
            client.send(Cat("Mr. Bigglesworth", 8))
            client.send(Snake("Buttercup", false))
            client.send(Snake("Monty the Python", true))

            then("the messages are received") {
                val messages = listener.receivedAnimals
                eventually(10.seconds) {
                    messages.size shouldBe  4
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
    }
})
