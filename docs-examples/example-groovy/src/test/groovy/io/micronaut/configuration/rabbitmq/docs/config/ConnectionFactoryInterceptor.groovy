package io.micronaut.configuration.rabbitmq.docs.config

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.DefaultCredentialsProvider
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener

import javax.inject.Singleton

@Singleton
class ConnectionFactoryInterceptor implements BeanCreatedEventListener<ConnectionFactory> {

    @Override
    ConnectionFactory onCreated(BeanCreatedEvent<ConnectionFactory> event) {
        def connectionFactory = event.bean
        connectionFactory.credentialsProvider = new DefaultCredentialsProvider("guest", "guest")
        connectionFactory
    }
}
