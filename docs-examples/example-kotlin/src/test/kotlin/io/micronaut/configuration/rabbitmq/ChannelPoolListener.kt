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
            channel.queueDeclare("product", false, false, false, HashMap())

            //docs/exchange
            channel.exchangeDeclare("animals", "headers", false)
            channel.queueDeclare("snakes", false, false, false, null)
            channel.queueDeclare("cats", false, false, false, null)
            val catArgs = HashMap<String, Any>()
            catArgs["x-match"] = "all"
            catArgs["animalType"] = "Cat"
            channel.queueBind("cats", "animals", "", catArgs)

            val snakeArgs = HashMap<String, Any>()
            snakeArgs["x-match"] = "all"
            snakeArgs["animalType"] = "Snake"
            channel.queueBind("snakes", "animals", "", snakeArgs)

            pool.returnChannel(channel)
        } catch (e: IOException) {
            //no-op
        }

        return pool
    }

}
