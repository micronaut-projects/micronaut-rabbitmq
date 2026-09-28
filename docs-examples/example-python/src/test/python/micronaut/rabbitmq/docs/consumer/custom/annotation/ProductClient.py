from abc import ABC, abstractmethod

from micronaut.context.annotation import Requires
from micronaut.rabbitmq.annotation import Binding, RabbitClient


@Requires(property="spec.name", value="DeliveryTagSpec")
@RabbitClient
class ProductClient(ABC):

    @Binding("product")
    @abstractmethod
    def send(self, data: bytes) -> None:
        ...
