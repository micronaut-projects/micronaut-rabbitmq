from abc import ABC, abstractmethod
from typing import Annotated

from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.messaging.annotation import MessageBody
from micronaut.rabbitmq.annotation import Binding, RabbitClient

from .ProductInfo import ProductInfo
# end::imports[]


@Requires(property="spec.name", value="ProductInfoSerDesSpec")
# tag::clazz[]
@RabbitClient
class ProductClient(ABC):

    @Binding("product")
    @abstractmethod
    def send(self, data: Annotated[ProductInfo, MessageBody]) -> None:
        ...
# end::clazz[]
