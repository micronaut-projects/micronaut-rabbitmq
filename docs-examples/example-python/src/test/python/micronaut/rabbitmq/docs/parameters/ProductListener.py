from micronaut.context.annotation import Requires
# tag::imports[]
from micronaut.rabbitmq.annotation import Queue, RabbitListener
# end::imports[]


@Requires(property="spec.name", value="BindingSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.message_lengths: list[int] = []

    @Queue("product")  # <1>
    def receive(self, data: bytes) -> None:
        self.message_lengths.append(len(data))
# end::clazz[]
