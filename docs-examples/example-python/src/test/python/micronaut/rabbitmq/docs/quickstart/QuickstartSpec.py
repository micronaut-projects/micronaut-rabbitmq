from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ProductClient import ProductClient
from .ProductListener import ProductListener


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="QuickstartSpec")
class QuickstartSpec:
    product_client: Annotated[ProductClient, Inject]
    product_listener: Annotated[ProductListener, Inject]

    @Test
    def test_product_client_and_listener(self) -> None:
# tag::producer[]
        self.product_client.send(b"quickstart")
# end::producer[]

        Await.until(lambda: self.product_listener.message_lengths == ["quickstart"])
