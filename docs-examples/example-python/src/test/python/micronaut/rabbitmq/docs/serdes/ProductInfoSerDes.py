from micronaut.context.annotation import Requires
# tag::imports[]
from jakarta.inject import Singleton
from java.lang import Boolean, Long
from micronaut.core.convert import ConversionService
from micronaut.core.type import Argument
from micronaut.rabbitmq.bind import RabbitConsumerState
from micronaut.rabbitmq.intercept import MutableBasicProperties
from micronaut.rabbitmq.serdes import RabbitMessageSerDes

from .ProductInfo import ProductInfo
# end::imports[]


@Requires(property="spec.name", value="ProductInfoSerDesSpec")
# tag::clazz[]
@Singleton  # <1>
class ProductInfoSerDes(RabbitMessageSerDes[ProductInfo]):  # <2>

    def __init__(self, conversion_service: ConversionService):  # <3>
        self.conversion_service = conversion_service

    def deserialize(self, consumer_state: RabbitConsumerState, argument: Argument[ProductInfo]) -> ProductInfo | None:  # <4>
        body = bytes(consumer_state.getBody()).decode("utf-8")
        parts = body.split("|")
        if len(parts) == 3:
            size = parts[0]
            if size == "null":
                size = None

            count = self.conversion_service.convert(parts[1], Long)
            sealed = self.conversion_service.convert(parts[2], Boolean)

            if count.isPresent() and sealed.isPresent():
                return ProductInfo(size, count.get(), sealed.get())
        return None

    def serialize(self, data: ProductInfo | None, properties: MutableBasicProperties) -> bytes | None:  # <5>
        if data is None:
            return None
        size = "null" if data.size is None else data.size
        sealed = str(data.sealed).lower()
        return f"{size}|{data.count}|{sealed}".encode("utf-8")

    def supports(self, argument: Argument[ProductInfo]) -> bool:  # <6>
        return argument.getType().isAssignableFrom(ProductInfo)
# end::clazz[]
