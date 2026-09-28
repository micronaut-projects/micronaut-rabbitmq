from typing import Annotated

from micronaut.context.annotation import Requires
# tag::imports[]
from java.lang import Long
from micronaut.messaging.annotation import MessageHeader
from micronaut.rabbitmq.annotation import Queue, RabbitHeaders, RabbitListener
# end::imports[]


@Requires(property="spec.name", value="HeadersSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.message_properties: list[str] = []

    @Queue("product")
    def receive(self, data: bytes,
                sealed: Annotated[bool, MessageHeader("x-product-sealed")],  # <1>
                count: Annotated[Long, MessageHeader("x-product-count")],  # <2>
                product_size: Annotated[str | None, MessageHeader("productSize")]) -> None:  # <3>
        self.message_properties.append(f"{str(sealed).lower()}|{count}|{product_size}")

    @Queue("product")
    def receive_headers(self, data: bytes,
                        headers: Annotated[dict[str, object], RabbitHeaders]) -> None:  # <4>
        product_size = headers.get("productSize")
        self.message_properties.append(
            f"{headers.get('x-product-sealed')}|{headers.get('x-product-count')}|{product_size}")
# end::clazz[]
