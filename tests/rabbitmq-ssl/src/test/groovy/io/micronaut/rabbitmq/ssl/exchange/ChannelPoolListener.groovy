package io.micronaut.rabbitmq.ssl.exchange

import com.rabbitmq.client.Channel
import io.micronaut.context.annotation.Requires
import io.micronaut.rabbitmq.connect.ChannelInitializer
import jakarta.inject.Singleton

@Requires(property = "spec.name", value = "CustomSSLExchangeSpec")
@Singleton
class ChannelPoolListener extends ChannelInitializer {

    @Override
    void initialize(Channel channel, String name) throws IOException {

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
