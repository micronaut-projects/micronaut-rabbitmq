package io.micronaut.rabbitmq;

// tag::clazz[]
import com.rabbitmq.client.Channel;
import io.micronaut.rabbitmq.connect.ChannelInitializer;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Singleton // <1>
public class ChannelPoolListener extends ChannelInitializer { // <2>

    @Override
    public void initialize(Channel channel) throws IOException { // <3>
        //docs/quickstart
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", 100);
        channel.queueDeclare("product", false, false, false, args); // <4>

        //docs/exchange
        channel.exchangeDeclare("animals", "headers", false);
        channel.queueDeclare("snakes", false, false, false, null);
        channel.queueDeclare("cats", false, false, false, null);
        Map<String, Object> catArgs = new HashMap<>();
        catArgs.put("x-match", "all");
        catArgs.put("animalType", "Cat");
        channel.queueBind("cats", "animals", "", catArgs);

        Map<String, Object> snakeArgs = new HashMap<>();
        snakeArgs.put("x-match", "all");
        snakeArgs.put("animalType", "Snake");
        channel.queueBind("snakes", "animals", "", snakeArgs);
    }

}
// end::clazz[]