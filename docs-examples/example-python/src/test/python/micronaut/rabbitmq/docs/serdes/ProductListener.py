from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Queue, RabbitListener

from .ProductInfo import ProductInfo
# end::imports[]


@Requires(property="spec.name", value="ProductInfoSerDesSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.messages: list[ProductInfo] = []

    @Queue("product")
    def receive(self, product_info: ProductInfo) -> None:  # <1>
        self.messages.append(product_info)
# end::clazz[]
