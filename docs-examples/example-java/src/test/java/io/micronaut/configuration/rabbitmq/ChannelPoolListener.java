package io.micronaut.configuration.rabbitmq;

import com.rabbitmq.client.Channel;
import io.micronaut.configuration.rabbitmq.connect.ChannelPool;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;

@Singleton
public class ChannelPoolListener implements BeanCreatedEventListener<ChannelPool> {

    @Override
    public ChannelPool onCreated(BeanCreatedEvent<ChannelPool> event) {
        ChannelPool pool = event.getBean();
        try {
            Channel channel = pool.getChannel();
            channel.queueDeclare("product", true, false, false, new HashMap());
            pool.returnChannel(channel);
        } catch (IOException e) {
            //no-op
        }

        return pool;
    }

}
