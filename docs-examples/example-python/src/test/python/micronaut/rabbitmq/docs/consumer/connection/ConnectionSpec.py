from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .ProductClient import ProductClient
from .ProductListener import ProductListener


# The "rabbitmq-cluster" environment configures the test container as the "product-cluster" server
# (see the Java RabbitMQTestConfigurer of this project)
@MicronautTest(environments=["rabbitmq", "rabbitmq-cluster"])
@Property(name="spec.name", value="ConnectionSpec")
class ConnectionSpec:
    product_client: Annotated[ProductClient, Inject]
    product_listener: Annotated[ProductListener, Inject]

    @Test
    def test_product_client_and_listener(self) -> None:
# tag::producer[]
        self.product_client.send(b"connection-test")
# end::producer[]

        Await.until(lambda: self.product_listener.message_lengths == ["connection-test"], timeout=10)
