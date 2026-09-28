from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Queue, RabbitListener
# end::imports[]


@Requires(property="spec.name", value="QuickstartSpec")
# tag::clazz[]
@RabbitListener  # <1>
class ProductListener:

    def __init__(self) -> None:
        self.message_lengths: list[str] = []

    @Queue("product")  # <2>
    def receive(self, data: bytes) -> None:  # <3>
        self.message_lengths.append(bytes(data).decode())
        print(f"Python received {len(data)} bytes from RabbitMQ")
# end::clazz[]
