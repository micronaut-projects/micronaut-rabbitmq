from abc import ABC, abstractmethod
from typing import Annotated

from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Binding, RabbitClient
# end::imports[]


@Requires(property="spec.name", value="BindingSpec")
# tag::clazz[]
@RabbitClient
class ProductClient(ABC):

    @Binding("product")  # <1>
    @abstractmethod
    def send(self, data: bytes) -> None:
        ...

    @abstractmethod
    def send_to(self, binding: Annotated[str, Binding], data: bytes) -> None:  # <2>
        ...
# end::clazz[]
