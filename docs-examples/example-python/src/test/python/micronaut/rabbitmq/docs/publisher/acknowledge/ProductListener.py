from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Queue, RabbitListener
# end::imports[]


@Requires(property="spec.name", value="PublisherAcknowledgeSpec")
# tag::clazz[]
@RabbitListener  # <1>
class ProductListener:

    def __init__(self) -> None:
        self.message_lengths: list[int] = []

    @Queue("product")  # <2>
    def receive(self, data: bytes) -> None:  # <3>
        length = len(data)
        self.message_lengths.append(length)
        print(f"Python received {length} bytes from RabbitMQ")
# end::clazz[]
