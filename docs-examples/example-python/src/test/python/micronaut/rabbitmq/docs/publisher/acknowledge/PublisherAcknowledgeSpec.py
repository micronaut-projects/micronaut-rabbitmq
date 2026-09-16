from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.rabbitmq.docs.Await import Await
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test
from org.reactivestreams import Subscriber, Subscription

from .ProductClient import ProductClient


@MicronautTest(environments=["rabbitmq"])
@Property(name="spec.name", value="PublisherAcknowledgeSpec")
class PublisherAcknowledgeSpec:
    product_client: Annotated[ProductClient, Inject]

    @Test
    def test_publisher_acknowledgement(self) -> None:
        counts = {"success": 0, "error": 0}

# tag::producer[]
        publisher = self.product_client.send_publisher(b"publisher body")
        future = self.product_client.send_future(b"future body")

        class AcknowledgementSubscriber(Subscriber):

            def onSubscribe(self, subscription: Subscription) -> None:
                pass

            def onNext(self, item: object) -> None:
                raise RuntimeError("Should never be called")

            def onError(self, throwable: Exception) -> None:
                # if an error occurs
                counts["error"] += 1

            def onComplete(self) -> None:
                # if the publish was acknowledged
                counts["success"] += 1

        publisher.subscribe(AcknowledgementSubscriber())

        def completed(value, throwable) -> None:
            if throwable is None:
                counts["success"] += 1
            else:
                counts["error"] += 1

        future.whenComplete(completed)
# end::producer[]

        Await.until(lambda: counts["error"] == 0 and counts["success"] == 2)
