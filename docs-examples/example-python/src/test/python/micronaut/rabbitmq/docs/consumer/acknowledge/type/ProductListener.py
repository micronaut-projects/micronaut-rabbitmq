from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Queue, RabbitListener
from micronaut.rabbitmq.bind import RabbitAcknowledgement
# end::imports[]


@Requires(property="spec.name", value="AcknowledgeSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.message_count = 0

    @Queue(value="product")  # <1>
    def receive(self, data: bytes, acknowledgement: RabbitAcknowledgement) -> None:  # <2>
        count = self.message_count
        self.message_count += 1
        if count == 0:
            acknowledgement.nack(False, True)  # <3>
        elif count > 3:
            acknowledgement.ack(True)  # <4>
# end::clazz[]
