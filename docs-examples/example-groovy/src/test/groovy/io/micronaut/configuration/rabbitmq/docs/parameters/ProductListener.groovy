package io.micronaut.configuration.rabbitmq.docs.parameters;

import io.micronaut.configuration.rabbitmq.annotation.Queue;
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener;
import io.micronaut.context.annotation.Requires;

import java.util.ArrayList;
import java.util.List;

@Requires(property = "spec.name", value = "BindingSpec")
@RabbitListener
public class ProductListener {

    List<Integer> messageLengths = new ArrayList<>();

    @Queue("product")
    public void receive(byte[] data) {
        messageLengths.add(data.length);
    }
}
