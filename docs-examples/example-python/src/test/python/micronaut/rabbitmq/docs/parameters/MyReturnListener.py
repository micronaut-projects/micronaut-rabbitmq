from micronaut.context.annotation import Requires
# tag::imports[]
from com.rabbitmq.client import AMQP, Channel, ReturnListener
from jakarta.inject import Singleton
from micronaut.rabbitmq.connect import ChannelPoolInitializer
# end::imports[]


@Requires(property="spec.name", value="MandatorySpec")
# tag::clazz[]
@Singleton
class MyReturnListener(ChannelPoolInitializer, ReturnListener):

    def initialize(self, channel: Channel, name: str) -> None:
        channel.addReturnListener(self)  # <1>

    def handleReturn(
        self,
        reply_code: int,
        reply_text: str,
        exchange: str,
        routing_key: str,
        properties: AMQP.BasicProperties,
        body: bytes
    ) -> None:
        pass  # <2>
# end::clazz[]
