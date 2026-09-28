from typing import Annotated

from micronaut.context.annotation import Requires
# tag::imports[]
from java.lang import Integer
from micronaut.rabbitmq.annotation import Queue, RabbitListener, RabbitProperty
# end::imports[]


@Requires(property="spec.name", value="PropertiesSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.message_properties: list[str] = []

    @Queue("product")
    @RabbitProperty(name="x-priority", value="10", type=Integer)  # <1>
    def receive(self, data: bytes,
                user: Annotated[str, RabbitProperty("userId")],  # <2>
                content_type: Annotated[str | None, RabbitProperty("contentType")],  # <3>
                appId: str) -> None:  # <4>
        self.message_properties.append(f"{user}|{content_type}|{appId}")
# end::clazz[]
