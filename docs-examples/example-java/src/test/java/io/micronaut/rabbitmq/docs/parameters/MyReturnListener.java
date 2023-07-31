package io.micronaut.rabbitmq.docs.parameters;

import io.micronaut.context.annotation.Requires;
// tag::imports[]
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ReturnListener;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import jakarta.inject.Singleton;

import java.io.IOException;
// end::imports[]

@Requires(property = "spec.name", value = "MandatorySpec")
// tag::clazz[]
@Singleton
class MyReturnListener extends ChannelInitializer implements ReturnListener {

    @Override
    public void initialize(Channel channel, String name) throws IOException {
        channel.addReturnListener(this); // <1>
    }

    @Override
    public void handleReturn(
        int replyCode,
        String replyText,
        String exchange,
        String routingKey,
        AMQP.BasicProperties properties,
        byte[] body
    ) throws IOException {
        // <2>
    }
}
// end::clazz[]
