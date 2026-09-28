from abc import ABC, abstractmethod

from micronaut.context.annotation import Requires
# tag::imports[]
from java.util.concurrent import CompletableFuture
from micronaut.rabbitmq.annotation import Binding, RabbitClient
from org.reactivestreams import Publisher
# end::imports[]


@Requires(property="spec.name", value="PublisherAcknowledgeSpec")
# tag::clazz[]
@RabbitClient
class ProductClient(ABC):

    @Binding("product")
    @abstractmethod
    def send_publisher(self, data: bytes) -> Publisher[None]:  # <1>
        ...

    @Binding("product")
    @abstractmethod
    def send_future(self, data: bytes) -> CompletableFuture[None]:  # <2>
        ...
# end::clazz[]
