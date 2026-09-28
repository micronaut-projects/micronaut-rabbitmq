from micronaut.context.annotation import Requires
# tag::imports[]
from jakarta.inject import Singleton
from java.lang import Boolean, Long, String
from java.util import Optional
from micronaut.core.bind.ArgumentBinder import BindingResult
from micronaut.core.convert import ArgumentConversionContext, ConversionError, ConversionService
from micronaut.core.type import Argument
from micronaut.rabbitmq.bind import RabbitConsumerState, RabbitHeaderConvertibleValues, RabbitTypeArgumentBinder

from .ProductInfo import ProductInfo
# end::imports[]


@Requires(property="spec.name", value="ProductInfoSpec")
# tag::clazz[]
@Singleton  # <1>
class ProductInfoTypeBinder(RabbitTypeArgumentBinder[ProductInfo]):  # <2>

    def __init__(self, conversion_service: ConversionService):  # <3>
        self.conversion_service = conversion_service

    def argumentType(self) -> Argument[ProductInfo]:
        return Argument.of(ProductInfo)

    def bind(self, context: ArgumentConversionContext[ProductInfo], source: RabbitConsumerState) -> BindingResult[ProductInfo]:
        raw_headers = source.getProperties().getHeaders()  # <4>

        if raw_headers is None:
            return BindingResult.empty()

        headers = RabbitHeaderConvertibleValues(raw_headers, self.conversion_service)

        size = headers.get("productSize", String).orElse(None)  # <5>
        count = headers.get("x-product-count", Long)  # <6>
        sealed = headers.get("x-product-sealed", Boolean)  # <7>

        if headers.getConversionErrors().isEmpty() and count.isPresent() and sealed.isPresent():
            return lambda: Optional.of(ProductInfo(size, count.get(), sealed.get()))  # <8>
        else:
            return self.ConversionErrorsResult(headers)

    class ConversionErrorsResult(BindingResult):

        def __init__(self, headers: RabbitHeaderConvertibleValues):
            self.headers = headers

        def getValue(self) -> Optional[ProductInfo]:
            return Optional.empty()

        def getConversionErrors(self) -> list[ConversionError]:
            return self.headers.getConversionErrors()  # <9>
# end::clazz[]
