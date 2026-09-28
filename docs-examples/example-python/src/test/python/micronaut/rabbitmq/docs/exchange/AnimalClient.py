from abc import ABC, abstractmethod
from typing import Annotated

from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.messaging.annotation import MessageHeader
from micronaut.rabbitmq.annotation import RabbitClient

from .Animal import Animal
# end::imports[]


@Requires(property="spec.name", value="CustomExchangeSpec")
# tag::clazz[]
@RabbitClient("animals")  # <1>
class AnimalClient(ABC):

    @abstractmethod
    def send(self, animal_type: Annotated[str, MessageHeader("animalType")], animal: Animal) -> None:  # <2>
        ...

    def send_animal(self, animal: Animal) -> None:  # <3>
        self.send(type(animal).__name__, animal)
# end::clazz[]
