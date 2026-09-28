from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ProductClient import ProductClient
from .ProductListener import ProductListener


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="BindingSpec")
class BindingSpec:
    product_client: Annotated[ProductClient, Inject]
    product_listener: Annotated[ProductListener, Inject]

    @Test
    def test_dynamic_binding(self) -> None:
# tag::producer[]
        self.product_client.send(b"message body")
        self.product_client.send_to("product", b"message body2")
# end::producer[]

        message_lengths = self.product_listener.message_lengths
        Await.until(lambda: len(message_lengths) == 2 and 12 in message_lengths and 13 in message_lengths)
