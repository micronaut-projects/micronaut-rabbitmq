package io.micronaut.rabbitmq.connect

import com.rabbitmq.client.Channel

import javax.inject.Singleton

@Singleton
class ChannelPoolListener extends ChannelInitializer {

    @Override
    void initialize(Channel channel) throws IOException {
        channel.queueDeclare("abc", false, false, false, new HashMap<>())
        channel.queueDeclare("pojo", false, false, false, new HashMap<>())
        channel.queueDeclare("pojo-list", false, false, false, new HashMap<>())
        channel.queueDeclare("simple", false, false, false, new HashMap<>())
        channel.queueDeclare("simple-list", false, false, false, new HashMap<>())
        channel.queueDeclare("header", false, false, false, new HashMap<>())
        channel.queueDeclare("property", false, false, false, new HashMap<>())
        channel.queueDeclare("type", false, false, false, new HashMap<>())
        channel.queueDeclare("boolean", false, false, false, new HashMap<>())
        channel.queueDeclare("product", false, false, false, new HashMap<>())
        channel.queueDeclare("rpc", false, false, false, new HashMap<>())

        channel.exchangeDeclare("animals", "headers", false);
        channel.queueDeclare("dogs", false, false, false, null);
        channel.queueDeclare("cats", false, false, false, null);
        Map<String, Object> catArgs = new HashMap<String, Object>()
        catArgs.put("x-match", "all")
        catArgs.put("animalType", "Cat")
        channel.queueBind("cats", "animals", "", catArgs)

        Map<String, Object> dogArgs = new HashMap<String, Object>()
        dogArgs.put("x-match", "all")
        dogArgs.put("animalType", "Dog")
        channel.queueBind("dogs", "animals", "", dogArgs)
    }
}
