from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ProductClient import ProductClient
from .ProductListener import ProductListener


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="AcknowledgeSpec")
class AcknowledgeSpec:
    product_client: Annotated[ProductClient, Inject]
    product_listener: Annotated[ProductListener, Inject]

    @Test
    def test_acking_with_acknowledgement(self) -> None:
# tag::producer[]
        self.product_client.send(b"message body")
        self.product_client.send(b"message body")
        self.product_client.send(b"message body")
        self.product_client.send(b"message body")
# end::producer[]

        # the first message is rejected and re-queued
        Await.until(lambda: self.product_listener.message_count == 5)
