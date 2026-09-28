from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Queue, RabbitListener

from .Animal import Animal
from .Cat import Cat
from .Snake import Snake
# end::imports[]


@Requires(property="spec.name", value="CustomExchangeSpec")
# tag::clazz[]
@RabbitListener  # <1>
class AnimalListener:

    def __init__(self) -> None:
        self.received_animals: list[Animal] = []

    @Queue("cats")  # <2>
    def receive_cat(self, cat: Cat) -> None:  # <3>
        self.received_animals.append(cat)

    @Queue("snakes")  # <2>
    def receive_snake(self, snake: Snake) -> None:  # <3>
        self.received_animals.append(snake)
# end::clazz[]
