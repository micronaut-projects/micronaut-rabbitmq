package io.micronaut.configuration.rabbitmq

// tag::clazz[]
import com.rabbitmq.client.Channel
import io.micronaut.configuration.rabbitmq.connect.ChannelPool
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener

import javax.inject.Singleton
import java.io.IOException
import java.util.HashMap

@Singleton // <1>
class ChannelPoolListener : BeanCreatedEventListener<ChannelPool> { // <2>

    override fun onCreated(event: BeanCreatedEvent<ChannelPool>): ChannelPool {
        val pool = event.bean // <3>

        var channel: Channel? = null

        try {
            channel = pool.channel // <4>
        } catch (e: IOException) {
            // The channel couldn't be retrieved
        }

        if (channel != null) {
            try {
                //docs/quickstart
                channel.queueDeclare("product", false, false, false, mapOf("x-max-priority" to 100)) // <5>

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

            } catch (e: IOException) {
                // An error occurred performing the operations
            } finally {
                pool.returnChannel(channel) // <6>
            }
        }

        return pool // <7>
    }

}
// end::clazz[]