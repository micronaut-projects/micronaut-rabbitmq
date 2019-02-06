package io.micronaut.configuration.rabbitmq

// tag::clazz[]
import com.rabbitmq.client.Channel
import io.micronaut.configuration.rabbitmq.connect.ChannelPool
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener

import javax.inject.Singleton

@Singleton // <1>
class ChannelPoolListener implements BeanCreatedEventListener<ChannelPool> { // <2>

    @Override
    ChannelPool onCreated(BeanCreatedEvent<ChannelPool> event) {
        ChannelPool pool = event.bean // <3>
        Channel channel = null
        try {
            channel = pool.channel // <4>
        } catch (IOException e) {
            // The channel couldn't be retrieved
        }

        if (channel != null) {
            try {
                channel.queueDeclare("product", false, false, false, ["x-max-priority": 100]) // <5>

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

            } catch (IOException e) {
                //no-op
            } finally {
                pool.returnChannel(channel) // <6>
            }
        }

        pool // <7>
    }
}
// end::clazz[]