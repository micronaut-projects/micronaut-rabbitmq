from abc import ABC, abstractmethod

from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Binding, RabbitClient
# end::imports[]


@Requires(property="spec.name", value="ConnectionSpec")
# tag::clazz[]
@RabbitClient  # <1>
class ProductClient(ABC):

    @Binding(value="product", connection="product-cluster")  # <2>
    @abstractmethod
    def send(self, data: bytes) -> None:  # <3>
        ...
# end::clazz[]
