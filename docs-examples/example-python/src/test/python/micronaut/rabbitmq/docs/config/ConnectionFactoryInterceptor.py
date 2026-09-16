from com.rabbitmq.client import ConnectionFactory
from com.rabbitmq.client.impl import DefaultCredentialsProvider
from jakarta.inject import Singleton
from micronaut.context.event import BeanCreatedEvent, BeanCreatedEventListener


@Singleton
class ConnectionFactoryInterceptor(BeanCreatedEventListener[ConnectionFactory]):

    def onCreated(self, event: BeanCreatedEvent[ConnectionFactory]) -> ConnectionFactory:
        connection_factory = event.getBean()
        connection_factory.setCredentialsProvider(DefaultCredentialsProvider("guest", "guest"))
        return connection_factory
