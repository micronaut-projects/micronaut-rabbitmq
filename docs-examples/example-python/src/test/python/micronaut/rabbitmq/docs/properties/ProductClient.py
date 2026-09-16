from abc import ABC, abstractmethod
from typing import Annotated

from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Binding, RabbitClient, RabbitProperty
# end::imports[]


@Requires(property="spec.name", value="PropertiesSpec")
# tag::clazz[]
@RabbitClient
@RabbitProperty(name="appId", value="myApp")  # <1>
@RabbitProperty(name="userId", value="admin")
class ProductClient(ABC):

    @Binding("product")
    @RabbitProperty(name="contentType", value="application/json")  # <2>
    @RabbitProperty(name="userId", value="guest")
    @abstractmethod
    def send(self, data: bytes) -> None:
        ...

    @Binding("product")
    @abstractmethod
    def send_with_properties(self, user: Annotated[str, RabbitProperty("userId")],
                             content_type: Annotated[str | None, RabbitProperty("contentType")],
                             data: bytes) -> None:  # <3>
        ...
# end::clazz[]
