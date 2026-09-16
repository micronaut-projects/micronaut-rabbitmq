from typing import Annotated

import java
from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .AnimalClient import AnimalClient
from .AnimalListener import AnimalListener
from .Cat import Cat
from .Snake import Snake

# TODO(python): java.type needed because the deserialized animals reach the listener as instances of the generated
# Java classes and are filtered with java.instanceof(...), whose type argument must be a Java class; the imported
# Python classes fail with "instanceof second argument 'type' is not a Java class"
CatClass = java.type("micronaut.rabbitmq.docs.exchange.Cat")
SnakeClass = java.type("micronaut.rabbitmq.docs.exchange.Snake")


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="CustomExchangeSpec")
class CustomExchangeSpec:
    client: Annotated[AnimalClient, Inject]
    listener: Annotated[AnimalListener, Inject]

    @Test
    def test_using_a_custom_exchange(self) -> None:
        self.client.send_animal(Cat("Whiskers", 9))
        self.client.send_animal(Cat("Mr. Bigglesworth", 8))
        self.client.send_animal(Snake("Buttercup", False))
        self.client.send_animal(Snake("Monty the Python", True))

        received = self.listener.received_animals
        cats = lambda: [cat for cat in received if java.instanceof(cat, CatClass)]
        snakes = lambda: [snake for snake in received if java.instanceof(snake, SnakeClass)]
        Await.until(lambda: len(received) == 4
                    and any(cat.name == "Whiskers" and cat.lives == 9 for cat in cats())
                    and any(cat.name == "Mr. Bigglesworth" and cat.lives == 8 for cat in cats())
                    and any(snake.name == "Buttercup" and not snake.venomous for snake in snakes())
                    and any(snake.name == "Monty the Python" and snake.venomous for snake in snakes()))
