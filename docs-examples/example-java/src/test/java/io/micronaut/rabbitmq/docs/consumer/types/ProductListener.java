package io.micronaut.rabbitmq.docs.consumer.types;

// tag::imports[]
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.context.annotation.Requires;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// end::imports[]

@Requires(property = "spec.name", value = "TypeBindingSpec")
// tag::clazz[]
@RabbitListener
public class ProductListener {

    List<String> messages = Collections.synchronizedList(new ArrayList<>());

    @Queue("product")
    public void receive(byte[] data,
                        Envelope envelope, // <1>
                        BasicProperties basicProperties, // <2>
                        Channel channel) { // <3>
        messages.add(String.format("exchange: [%s], routingKey: [%s], contentType: [%s]",
                envelope.getExchange(), envelope.getRoutingKey(), basicProperties.getContentType()));
    }
}
// end::clazz[]
