from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test
from reactor.core.publisher import Mono

from .ProductClient import ProductClient


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="RpcUppercaseSpec")
class RpcUppercaseSpec:
    product_client: Annotated[ProductClient, Inject]

    @Test
    def test_product_client_and_listener(self) -> None:
# tag::producer[]
        assert self.product_client.send("rpc") == "RPC"
        assert Mono.from_(self.product_client.send_reactive("hello")).block() == "HELLO"
# end::producer[]
