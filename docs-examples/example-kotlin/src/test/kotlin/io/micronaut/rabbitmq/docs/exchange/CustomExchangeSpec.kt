package io.micronaut.rabbitmq.docs.exchange

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import kotlin.time.Duration.Companion.seconds
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "CustomExchangeSpec")
class CustomExchangeSpec : TestPropertyProvider, AnnotationSpec() {

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }

    @Inject
    lateinit var client: AnimalClient

    @Inject
    lateinit var listener: AnimalListener

    @Test
    suspend fun testUsingCustomExchange() {
        client.send(Cat("Whiskers", 9))
        client.send(Cat("Mr. Bigglesworth", 8))
        client.send(Snake("Buttercup", false))
        client.send(Snake("Monty the Python", true))

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
