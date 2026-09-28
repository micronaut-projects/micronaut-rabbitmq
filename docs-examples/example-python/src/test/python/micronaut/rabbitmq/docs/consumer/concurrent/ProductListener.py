import time

# tag::imports[]
from java.lang import Thread
from micronaut.context.annotation import Requires
from micronaut.rabbitmq.annotation import Queue, RabbitListener
# end::imports[]


@Requires(property="spec.name", value="ConcurrentSpec")
# tag::clazz[]
@RabbitListener
class ProductListener:

    def __init__(self) -> None:
        self.threads: set[str] = set()

    @Queue(value="product", numberOfConsumers="5")  # <1>
    def receive(self, data: bytes) -> None:
        self.threads.add(Thread.currentThread().getName())  # <2>
        time.sleep(0.5)
# end::clazz[]
