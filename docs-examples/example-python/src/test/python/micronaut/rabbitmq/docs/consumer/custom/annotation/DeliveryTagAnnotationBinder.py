import java
from micronaut.context.annotation import Requires
# tag::imports[]
from jakarta.inject import Singleton
from micronaut.core.bind.ArgumentBinder import BindingResult
from micronaut.core.convert import ArgumentConversionContext, ConversionService
from micronaut.rabbitmq.bind import RabbitAnnotatedArgumentBinder, RabbitConsumerState

from .DeliveryTag import DeliveryTag
# end::imports[]

# TODO(python): java.type needed because the annotation type is returned to Java as a runtime java.lang.Class
# (RabbitAnnotatedArgumentBinder.getAnnotationType()); a Python-defined annotation function is not accepted
# ("Cannot convert '<function DeliveryTag>' (language: Python, type: function) to Java type 'java.lang.Class'")
DeliveryTagClass = java.type("micronaut.rabbitmq.docs.consumer.custom.annotation.DeliveryTag")


@Requires(property="spec.name", value="DeliveryTagSpec")
# tag::clazz[]
@Singleton  # <1>
class DeliveryTagAnnotationBinder(RabbitAnnotatedArgumentBinder[DeliveryTag]):  # <2>

    def __init__(self, conversion_service: ConversionService):  # <3>
        self.conversion_service = conversion_service

    def getAnnotationType(self) -> type[DeliveryTag]:
        return DeliveryTagClass

    def bind(self, context: ArgumentConversionContext[object], source: RabbitConsumerState) -> BindingResult[object]:
        delivery_tag = source.getEnvelope().getDeliveryTag()  # <4>
        return lambda: self.conversion_service.convert(delivery_tag, context)  # <5>
# end::clazz[]
