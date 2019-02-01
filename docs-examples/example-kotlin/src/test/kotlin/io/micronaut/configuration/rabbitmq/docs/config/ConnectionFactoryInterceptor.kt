package io.micronaut.configuration.rabbitmq.docs.config

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.DefaultCredentialsProvider
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import javax.inject.Singleton

@Singleton
class ConnectionFactoryInterceptor: BeanCreatedEventListener<ConnectionFactory> {

    override fun onCreated(event: BeanCreatedEvent<ConnectionFactory>?): ConnectionFactory {
        val connectionFactory = event!!.bean
        connectionFactory.setCredentialsProvider(DefaultCredentialsProvider("guest", "guest"))
        return connectionFactory
    }
}