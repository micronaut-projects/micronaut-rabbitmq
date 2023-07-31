package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Return
import jakarta.inject.Singleton

@Singleton
class ChannelPoolListener extends ChannelInitializer {

    List<Return> returns = []

    @Override
    void initialize(Channel channel, String name) throws IOException {
        channel.queueDeclare("abc", false, false, false, [:])
        channel.queueDeclare("pojo", false, false, false, [:])
        channel.queueDeclare("pojo-list", false, false, false, [:])
        channel.queueDeclare("simple", false, false, false, [:])
        channel.queueDeclare("simple-list", false, false, false, [:])
        channel.queueDeclare("simple-header", false, false, false, [:])
        channel.queueDeclare("error", false, false, false, [:])
        channel.queueDeclare("header", false, false, false, [:])
        channel.queueDeclare("property", false, false, false, [:])
        channel.queueDeclare("type", false, false, false, [:])
        channel.queueDeclare("boolean", false, false, false, [:])
        channel.queueDeclare("product", false, false, false, [:])
        channel.queueDeclare("rpc", false, false, false, [:])

        channel.exchangeDeclare("animals", "headers", false)
        channel.queueDeclare("dogs", false, false, false, null)
        channel.queueDeclare("cats", false, false, false, null)
        channel.queueBind("cats", "animals", "", ["x-match": "all", animalType: "Cat"])

        channel.queueBind("dogs", "animals", "", ["x-match": "all", animalType: "Dog"])

        channel.addReturnListener(r -> returns << r)
    }
}
