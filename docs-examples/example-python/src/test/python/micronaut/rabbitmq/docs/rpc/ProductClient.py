from abc import ABC, abstractmethod

from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Binding, RabbitClient, RabbitProperty
from org.reactivestreams import Publisher
# end::imports[]


@Requires(property="spec.name", value="RpcUppercaseSpec")
# tag::clazz[]
@RabbitClient
@RabbitProperty(name="replyTo", value="amq.rabbitmq.reply-to")  # <1>
class ProductClient(ABC):

    @Binding("product")
    @abstractmethod
    def send(self, data: str) -> str:  # <2>
        ...

    @Binding("product")
    @abstractmethod
    def send_reactive(self, data: str) -> Publisher[str]:  # <3>
        ...
# end::clazz[]
