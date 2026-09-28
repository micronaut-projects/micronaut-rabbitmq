from abc import ABC, abstractmethod

from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Binding, RabbitClient
# end::imports[]


@Requires(property="spec.name", value="TypeBindingSpec")
# tag::clazz[]
@RabbitClient  # <1>
class ProductClient(ABC):

    @Binding("product")  # <2>
    @abstractmethod
    def send(self, data: bytes, contentType: str) -> None:  # <3>
        ...
# end::clazz[]
