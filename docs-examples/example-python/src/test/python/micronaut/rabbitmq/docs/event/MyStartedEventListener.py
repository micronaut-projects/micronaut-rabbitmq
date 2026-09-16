from micronaut.context.annotation import Requires
# tag::imports[]
import logging

from jakarta.inject import Singleton
from micronaut.context.event import ApplicationEventListener
from micronaut.rabbitmq.event import RabbitConsumerStarted

LOG = logging.getLogger(__name__)
# end::imports[]


@Requires(property="spec.name", value="RabbitListenerEventsSpec")
# tag::clazz[]
@Singleton
class MyStartedEventListener(ApplicationEventListener[RabbitConsumerStarted]):

    def onApplicationEvent(self, event: RabbitConsumerStarted) -> None:
        LOG.info("RabbitMQ consumer: %s (method: %s) just subscribed to: %s",
                 event.getSource(), event.getMethod(), event.getQueue())
# end::clazz[]
