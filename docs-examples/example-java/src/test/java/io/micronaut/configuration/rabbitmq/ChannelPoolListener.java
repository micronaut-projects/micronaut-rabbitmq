package io.micronaut.configuration.rabbitmq;

import com.rabbitmq.client.Channel;
import io.micronaut.configuration.rabbitmq.connect.ChannelPool;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ChannelPoolListener implements BeanCreatedEventListener<ChannelPool> {

    @Override
    public ChannelPool onCreated(BeanCreatedEvent<ChannelPool> event) {
        ChannelPool pool = event.getBean();
        try {
            Channel channel = pool.getChannel();
            //docs/quickstart
            Map<String, Object> args = new HashMap<>();
            args.put("x-max-priority", 100);
            channel.queueDeclare("product", false, false, false, args);

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

            pool.returnChannel(channel);
        } catch (IOException e) {
            //no-op
        }

        return pool;
    }

}
