from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Queue, RabbitListener
# end::imports[]


@Requires(property="spec.name", value="CustomExecutorSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.message_lengths: list[str] = []

    @Queue(value="product", executor="product-listener")  # <1>
    def receive(self, data: bytes) -> None:
        self.message_lengths.append(bytes(data).decode())
        print(f"Python received {len(data)} bytes from RabbitMQ")
# end::clazz[]
