from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .AnimalClient import AnimalClient
from .AnimalListener import AnimalListener
from .Cat import Cat
from .Snake import Snake


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
        cats = lambda: [cat for cat in received if isinstance(cat, Cat)]
        snakes = lambda: [snake for snake in received if isinstance(snake, Snake)]
        Await.until(lambda: len(received) == 4
                    and any(cat.name == "Whiskers" and cat.lives == 9 for cat in cats())
                    and any(cat.name == "Mr. Bigglesworth" and cat.lives == 8 for cat in cats())
                    and any(snake.name == "Buttercup" and not snake.venomous for snake in snakes())
                    and any(snake.name == "Monty the Python" and snake.venomous for snake in snakes()))
