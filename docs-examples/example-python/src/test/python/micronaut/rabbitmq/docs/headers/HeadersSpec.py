from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ProductClient import ProductClient
from .ProductListener import ProductListener


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="HeadersSpec")
class HeadersSpec:
    product_client: Annotated[ProductClient, Inject]
    product_listener: Annotated[ProductListener, Inject]

    @Test
    def test_publishing_and_receiving_headers(self) -> None:
# tag::producer[]
        self.product_client.send(b"body")
        self.product_client.send_with_headers("medium", 20, b"body2")
        self.product_client.send_with_headers(None, 30, b"body3")

        headers = {"productSize": "large", "x-product-count": 40}
        self.product_client.send_with_header_map(headers, b"body4")
# end::producer[]

        message_properties = self.product_listener.message_properties
        Await.until(lambda: len(message_properties) == 4
                    and "true|10|small" in message_properties
                    and "true|20|medium" in message_properties
                    and "true|30|None" in message_properties
                    and "true|40|large" in message_properties)
