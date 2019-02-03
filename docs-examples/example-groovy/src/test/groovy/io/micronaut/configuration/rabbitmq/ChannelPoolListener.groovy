package io.micronaut.configuration.rabbitmq

import com.rabbitmq.client.Channel
import io.micronaut.configuration.rabbitmq.connect.ChannelPool
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener

import javax.inject.Singleton

@Singleton
class ChannelPoolListener implements BeanCreatedEventListener<ChannelPool> {

    @Override
    ChannelPool onCreated(BeanCreatedEvent<ChannelPool> event) {
        ChannelPool pool = event.bean
        try {
            Channel channel = pool.channel
            channel.queueDeclare("product", false, false, false, [:])

            //docs/exchange
            channel.exchangeDeclare("animals", "headers", false)
            channel.queueDeclare("snakes", false, false, false, null)
            channel.queueDeclare("cats", false, false, false, null)
            Map<String, Object> catArgs = [:]
            catArgs.put("x-match", "all")
            catArgs.put("animalType", "Cat")
            channel.queueBind("cats", "animals", "", catArgs)

            Map<String, Object> snakeArgs = [:]
            snakeArgs.put("x-match", "all")
            snakeArgs.put("animalType", "Snake")
            channel.queueBind("snakes", "animals", "", snakeArgs)

            pool.returnChannel(channel)
        } catch (IOException e) {
            //no-op
        }
        pool
    }
}
