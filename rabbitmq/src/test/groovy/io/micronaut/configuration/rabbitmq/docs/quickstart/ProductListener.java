package io.micronaut.configuration.rabbitmq.docs.quickstart;

// tag::imports[]
import io.micronaut.configuration.rabbitmq.annotation.Queue;
import io.micronaut.configuration.rabbitmq.annotation.RabbitListener;
import io.micronaut.context.annotation.Requires;

import java.util.ArrayList;
import java.util.List;
// end::imports[]

@Requires(property = "spec.name", value = "QuickstartSpec")
// tag::class[]
@RabbitListener // <1>
public class ProductListener {

    public List<Integer> messageLengths = new ArrayList<>();

    @Queue("product") // <2>
    void receive(byte[] data) { // <3>
        Integer length = data.length;
        messageLengths.add(length);
        System.out.println("Received " + length + " bytes from RabbitMQ");
    }
}
// end::class[]
