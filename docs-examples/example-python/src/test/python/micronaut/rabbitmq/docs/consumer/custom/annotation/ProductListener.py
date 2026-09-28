from typing import Annotated

from micronaut.context.annotation import Requires
# tag::imports[]
from java.lang import Long
from micronaut.rabbitmq.annotation import Queue, RabbitListener

from .DeliveryTag import DeliveryTag
# end::imports[]


@Requires(property="spec.name", value="DeliveryTagSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.messages: set[int] = set()

    @Queue("product")
    def receive(self, data: bytes, tag: Annotated[Long, DeliveryTag]) -> None:  # <1>
        self.messages.add(tag)
# end::clazz[]
