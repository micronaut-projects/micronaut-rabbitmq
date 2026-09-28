from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ProductClient import ProductClient
from .ProductListener import ProductListener


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="TypeBindingSpec")
class TypeBindingSpec:
    product_client: Annotated[ProductClient, Inject]
    product_listener: Annotated[ProductListener, Inject]

    @Test
    def test_binding_by_type(self) -> None:
# tag::producer[]
        self.product_client.send(b"body", "text/html")
        self.product_client.send(b"body2", "application/json")
        self.product_client.send(b"body3", "text/xml")
# end::producer[]

        messages = self.product_listener.messages
        Await.until(lambda: len(messages) == 3
                    and "exchange: [], routingKey: [product], contentType: [text/html]" in messages
                    and "exchange: [], routingKey: [product], contentType: [application/json]" in messages
                    and "exchange: [], routingKey: [product], contentType: [text/xml]" in messages)
