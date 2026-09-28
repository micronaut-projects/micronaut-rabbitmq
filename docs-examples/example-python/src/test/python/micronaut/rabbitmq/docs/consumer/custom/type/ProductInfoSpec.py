from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ProductClient import ProductClient
from .ProductListener import ProductListener


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="ProductInfoSpec")
class ProductInfoSpec:
    product_client: Annotated[ProductClient, Inject]
    product_listener: Annotated[ProductListener, Inject]

    @Test
    def test_using_a_custom_type_binder(self) -> None:
# tag::producer[]
        self.product_client.send(b"body")
        self.product_client.send_with_headers("medium", 20, b"body2")
        self.product_client.send_with_headers(None, 30, b"body3")
# end::producer[]

        messages = self.product_listener.messages
        Await.until(lambda: len(messages) == 3
                    and any(pi.count == 10 for pi in messages)
                    and any(pi.count == 20 for pi in messages)
                    and any(pi.count == 30 for pi in messages))
