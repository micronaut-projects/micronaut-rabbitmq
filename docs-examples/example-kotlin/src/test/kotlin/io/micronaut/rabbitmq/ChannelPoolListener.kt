package io.micronaut.rabbitmq

// tag::clazz[]
import com.rabbitmq.client.Channel
import io.micronaut.rabbitmq.connect.ChannelInitializer
import java.io.IOException
import jakarta.inject.Singleton

@Singleton // <1>
class ChannelPoolListener : ChannelInitializer() { // <2>

    @Throws(IOException::class)
    override fun initialize(channel: Channel) { // <3>
        channel.queueDeclare("product", false, false, false, mapOf("x-max-priority" to 100)) // <4>

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
    }

}
// end::clazz[]
