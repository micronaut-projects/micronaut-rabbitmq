from micronaut.context.annotation import Requires
# tag::imports[]
from com.rabbitmq.client import BasicProperties, Channel, Envelope
from micronaut.rabbitmq.annotation import Queue, RabbitListener
# end::imports[]


@Requires(property="spec.name", value="TypeBindingSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.messages: list[str] = []

    @Queue("product")
    def receive(self, data: bytes,
                envelope: Envelope,  # <1>
                basic_properties: BasicProperties,  # <2>
                channel: Channel) -> None:  # <3>
        self.messages.append(f"exchange: [{envelope.getExchange()}], routingKey: [{envelope.getRoutingKey()}], "
                             f"contentType: [{basic_properties.getContentType()}]")
# end::clazz[]
