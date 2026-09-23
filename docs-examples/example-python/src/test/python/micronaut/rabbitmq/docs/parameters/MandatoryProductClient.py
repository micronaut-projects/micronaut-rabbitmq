from abc import ABC, abstractmethod
from typing import Annotated

from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Binding, Mandatory, RabbitClient
# end::imports[]


@Requires(property="spec.name", value="MandatorySpec")
# tag::clazz[]
@RabbitClient
class MandatoryProductClient(ABC):

    @Binding("product")
    @Mandatory  # <1>
    @abstractmethod
    def send(self, data: bytes) -> None:
        ...

    @Binding("product")
    @abstractmethod
    def send_mandatory(self, mandatory: Annotated[bool, Mandatory], data: bytes) -> None:  # <2>
        ...
# end::clazz[]
