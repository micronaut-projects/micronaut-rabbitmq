from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ProductClient import ProductClient
from .ProductListener import ProductListener


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="PropertiesSpec")
class PropertiesSpec:
    product_client: Annotated[ProductClient, Inject]
    product_listener: Annotated[ProductListener, Inject]

    @Test
    def test_publishing_and_receiving_properties(self) -> None:
# tag::producer[]
        self.product_client.send(b"body")
        self.product_client.send_with_properties("guest", "text/html", b"body2")
        self.product_client.send_with_properties("guest", None, b"body3")
# end::producer[]

        message_properties = self.product_listener.message_properties
        Await.until(lambda: len(message_properties) == 3
                    and "guest|application/json|myApp" in message_properties
                    and "guest|text/html|myApp" in message_properties
                    and "guest|None|myApp" in message_properties)
