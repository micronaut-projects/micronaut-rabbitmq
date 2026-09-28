from micronaut.context.annotation import Requires
# tag::imports[]
import logging

from jakarta.inject import Singleton
from micronaut.context.event import ApplicationEventListener
from micronaut.rabbitmq.event import RabbitConsumerStarting

LOG = logging.getLogger(__name__)
# end::imports[]


@Requires(property="spec.name", value="RabbitListenerEventsSpec")
# tag::clazz[]
@Singleton
class MyStartingEventListener(ApplicationEventListener[RabbitConsumerStarting]):

    def onApplicationEvent(self, event: RabbitConsumerStarting) -> None:
        LOG.info("RabbitMQ consumer: %s (method: %s) is subscribing to: %s",
                 event.getSource(), event.getMethod(), event.getQueue())
# end::clazz[]
