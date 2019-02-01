package io.micronaut.configuration.rabbitmq

import io.micronaut.configuration.rabbitmq.connect.ChannelPool
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener

import javax.inject.Singleton
import java.io.IOException
import java.util.HashMap

@Singleton
class ChannelPoolListener : BeanCreatedEventListener<ChannelPool> {

    override fun onCreated(event: BeanCreatedEvent<ChannelPool>): ChannelPool {
        val pool = event.bean
        try {
            val channel = pool.channel
            channel.queueDeclare("product", true, false, false, HashMap())
            pool.returnChannel(channel)
        } catch (e: IOException) {
            //no-op
        }

        return pool
    }

}
