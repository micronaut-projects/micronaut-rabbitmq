package io.micronaut.rabbitmq

// tag::clazz[]
import com.rabbitmq.client.Channel
import io.micronaut.rabbitmq.connect.ChannelInitializer

import javax.inject.Singleton

@Singleton // <1>
class ChannelPoolListener extends ChannelInitializer { // <2>

    @Override
    void initialize(Channel channel) throws IOException { // <3>
        channel.queueDeclare("product", false, false, false, ["x-max-priority": 100]) // <4>

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
    }
}
// end::clazz[]